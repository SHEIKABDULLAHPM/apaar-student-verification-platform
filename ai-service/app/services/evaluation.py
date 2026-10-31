"""Synthetic-only evaluation of the anomaly workflow.

This module deliberately evaluates on caller-supplied synthetic labels. It is
not a real-world validation harness and must not be presented as one.
"""

from __future__ import annotations

import numpy as np
import pandas as pd
from sklearn.ensemble import IsolationForest
from sklearn.metrics import confusion_matrix, f1_score, precision_score, recall_score
from sklearn.preprocessing import StandardScaler

from app.schemas.evaluation import (
    ConfusionMatrix,
    EvaluateResponse,
    EvaluationMetrics,
    LabelledFeatures,
)

EVALUATION_NOTE = (
    "Synthetic evaluation only. These metrics describe behaviour on the supplied "
    "synthetic sample and do not validate real-world accuracy, fairness, or "
    "regulatory compliance. IsolationForest is fitted in-sample here; use a "
    "separate hold-out protocol and institutional review before any operational use."
)


def _scores(feature_matrix: np.ndarray) -> np.ndarray:
    rows = feature_matrix.shape[0]
    if rows < 2 or feature_matrix.shape[1] == 0:
        return np.zeros(rows, dtype=float)

    scaled = StandardScaler().fit_transform(feature_matrix)
    if not np.isfinite(scaled).all() or np.allclose(scaled, 0.0):
        return np.zeros(rows, dtype=float)

    model = IsolationForest(
        n_estimators=200,
        contamination="auto",
        random_state=42,
        n_jobs=1,
    ).fit(scaled)
    raw = -model.score_samples(scaled)
    low = float(np.min(raw))
    high = float(np.max(raw))
    if high - low < 1e-12:
        return np.zeros(rows, dtype=float)
    return np.clip((raw - low) / (high - low), 0.0, 1.0)


def evaluate_labelled(
    records: list[LabelledFeatures],
    threshold: float,
    model_version: str,
) -> EvaluateResponse:
    """Compute precision/recall/F1/confusion for synthetic labelled features."""

    feature_names = sorted({name for record in records for name in record.features})
    frame = pd.DataFrame(
        [
            {name: float(record.features.get(name, 0.0)) for name in feature_names}
            for record in records
        ],
        columns=feature_names,
    )
    matrix = frame.to_numpy(dtype=float)
    if not np.isfinite(matrix).all():
        raise ValueError("features must contain only finite numbers")

    y_true = np.array([record.label for record in records], dtype=int)
    scores = _scores(matrix)
    y_pred = (scores >= threshold).astype(int)

    tn, fp, fn, tp = confusion_matrix(y_true, y_pred, labels=[0, 1]).ravel()
    precision = float(precision_score(y_true, y_pred, zero_division=0))
    recall = float(recall_score(y_true, y_pred, zero_division=0))
    f1 = float(f1_score(y_true, y_pred, zero_division=0))
    accuracy = float((y_true == y_pred).mean()) if len(y_true) else 0.0

    return EvaluateResponse(
        model_version=model_version,
        threshold=float(threshold),
        metrics=EvaluationMetrics(
            precision=round(precision, 4),
            recall=round(recall, 4),
            f1=round(f1, 4),
            accuracy=round(accuracy, 4),
            support=int(len(y_true)),
            positives=int(y_true.sum()),
            predicted_positive=int(y_pred.sum()),
        ),
        confusion_matrix=ConfusionMatrix(
            true_negative=int(tn),
            false_positive=int(fp),
            false_negative=int(fn),
            true_positive=int(tp),
            matrix=[[int(tn), int(fp)], [int(fn), int(tp)]],
        ),
        note=EVALUATION_NOTE,
    )
