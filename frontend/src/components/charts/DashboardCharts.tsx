import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import type {
  CountByCategory,
  IntakeVsEnrolment,
  VerificationActivity,
} from '@/api/types';
import { formatNumber, formatShortDate, truncate } from '@/lib/format';

const COLORS = ['#2c5d94', '#c05f05', '#15803d', '#0369a1', '#7c3aed', '#b45309', '#0f766e', '#be123c'];

function DataDetails({
  summary,
  headers,
  rows,
}: {
  summary: string;
  headers: string[];
  rows: Array<Array<string | number>>;
}) {
  return (
    <details className="mt-4 rounded-lg border border-line bg-surface-muted/40 px-3 py-2 text-sm">
      <summary className="cursor-pointer select-none font-medium text-ink-700">{summary}</summary>
      <div className="mt-3 overflow-x-auto">
        <table className="w-full min-w-[28rem] text-left text-xs">
          <thead>
            <tr className="border-b border-line text-ink-600">
              {headers.map((header) => (
                <th key={header} scope="col" className="px-2 py-2 font-semibold">
                  {header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map((row, index) => (
              <tr key={index} className="border-b border-line/60 last:border-0">
                {row.map((cell, cellIndex) => (
                  <td key={cellIndex} className="px-2 py-2 text-ink-700">
                    {typeof cell === 'number' ? formatNumber(cell) : cell}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </details>
  );
}

export function IntakeEnrolmentChart({ data }: { data: IntakeVsEnrolment[] }) {
  const chartData = data.map((row, index) => {
    const fullLabel = `${row.institutionName} · ${row.programmeName} · ${row.academicYear}`;
    return {
      key: String(index),
      label: truncate(fullLabel, 48),
      fullLabel,
      approvedIntake: row.approvedIntake,
      actualEnrolment: row.actualEnrolment,
    };
  });
  const height = Math.min(Math.max(chartData.length * 42, 260), 560);

  return (
    <figure aria-label="Approved intake compared with actual enrolment" className="m-0">
      <div style={{ height }} className="w-full">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart
            data={chartData}
            layout="vertical"
            margin={{ top: 4, right: 20, bottom: 4, left: 4 }}
            accessibilityLayer
          >
            <CartesianGrid strokeDasharray="3 3" stroke="#dbe3ec" horizontal={false} />
            <XAxis type="number" allowDecimals={false} tick={{ fontSize: 11, fill: '#475569' }} />
            <YAxis
              type="category"
              dataKey="label"
              width={200}
              tick={{ fontSize: 11, fill: '#334155' }}
            />
            <Tooltip
              labelFormatter={(_, payload) => {
                const item = payload?.[0]?.payload as { fullLabel?: string } | undefined;
                return item?.fullLabel ?? '';
              }}
              contentStyle={{ fontSize: 12, borderRadius: 8, borderColor: '#dbe3ec' }}
            />
            <Legend wrapperStyle={{ fontSize: 12 }} />
            <Bar
              dataKey="approvedIntake"
              name="Approved intake"
              fill={COLORS[0]}
              radius={[0, 4, 4, 0]}
              barSize={10}
            />
            <Bar
              dataKey="actualEnrolment"
              name="Actual enrolment"
              fill={COLORS[1]}
              radius={[0, 4, 4, 0]}
              barSize={10}
            />
          </BarChart>
        </ResponsiveContainer>
      </div>
      <figcaption className="sr-only">
        Bars compare sanctioned intake with recorded enrolment for each institution, programme and
        academic year returned by the server.
      </figcaption>
      <DataDetails
        summary="View chart data"
        headers={['Institution', 'Programme', 'Academic year', 'Approved intake', 'Enrolment']}
        rows={data.map((row) => [
          row.institutionName,
          row.programmeName,
          row.academicYear,
          row.approvedIntake,
          row.actualEnrolment,
        ])}
      />
    </figure>
  );
}

export function EnrolmentCategoryChart({ data }: { data: CountByCategory[] }) {
  const chartData = data.map((row) => ({ name: row.category, value: row.count }));

  return (
    <figure aria-label="Enrolments grouped by admission category" className="m-0">
      <div className="h-72 w-full">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart accessibilityLayer>
            <Pie
              data={chartData}
              dataKey="value"
              nameKey="name"
              innerRadius={52}
              outerRadius={88}
              paddingAngle={1}
            >
              {chartData.map((entry, index) => (
                <Cell key={entry.name} fill={COLORS[index % COLORS.length]} />
              ))}
            </Pie>
            <Tooltip contentStyle={{ fontSize: 12, borderRadius: 8, borderColor: '#dbe3ec' }} />
            <Legend wrapperStyle={{ fontSize: 12 }} />
          </PieChart>
        </ResponsiveContainer>
      </div>
      <figcaption className="sr-only">
        A category breakdown of recorded enrolments. Exact counts are listed in the data table.
      </figcaption>
      <DataDetails
        summary="View chart data"
        headers={['Category', 'Enrolments']}
        rows={data.map((row) => [row.category, row.count])}
      />
    </figure>
  );
}

export function VerificationActivityChart({ data }: { data: VerificationActivity[] }) {
  const chartData = data.map((row) => ({
    date: row.date,
    label: formatShortDate(row.date),
    count: row.count,
  }));

  return (
    <figure aria-label="APAAR verification activity over time" className="m-0">
      <div className="h-72 w-full">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart
            data={chartData}
            margin={{ top: 8, right: 16, bottom: 4, left: 0 }}
            accessibilityLayer
          >
            <CartesianGrid strokeDasharray="3 3" stroke="#dbe3ec" />
            <XAxis dataKey="label" tick={{ fontSize: 11, fill: '#475569' }} />
            <YAxis allowDecimals={false} tick={{ fontSize: 11, fill: '#475569' }} />
            <Tooltip contentStyle={{ fontSize: 12, borderRadius: 8, borderColor: '#dbe3ec' }} />
            <Line
              type="monotone"
              dataKey="count"
              name="Verifications"
              stroke={COLORS[0]}
              strokeWidth={2}
              dot={{ r: 3, fill: COLORS[0] }}
              activeDot={{ r: 5 }}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
      <figcaption className="sr-only">
        Daily count of APAAR verification checks returned by the server.
      </figcaption>
      <DataDetails
        summary="View chart data"
        headers={['Date', 'Verifications']}
        rows={data.map((row) => [formatShortDate(row.date), row.count])}
      />
    </figure>
  );
}
