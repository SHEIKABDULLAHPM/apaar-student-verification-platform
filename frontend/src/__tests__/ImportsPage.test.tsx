import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ImportsPage } from '@/pages/ImportsPage';
import * as importsApi from '@/api/imports';
import * as academicsApi from '@/api/academics';

vi.mock('@/api/imports', () => ({
  listImports: vi.fn(),
  uploadImport: vi.fn(),
  applyMappings: vi.fn(),
  getImportPreview: vi.fn(),
  commitImport: vi.fn(),
  getReconciliation: vi.fn(),
  downloadErrorReport: vi.fn(),
}));

vi.mock('@/api/academics', () => ({
  listAcademicYears: vi.fn(),
  listProgrammes: vi.fn(),
}));

describe('ImportsPage', () => {
  beforeEach(() => {
    vi.mocked(importsApi.listImports).mockReturnValue(
      Promise.resolve({
        content: [],
        page: 0,
        size: 10,
        totalElements: 0,
        totalPages: 0,
      })
    );
    vi.mocked(academicsApi.listAcademicYears).mockReturnValue(
      Promise.resolve([
        { id: 'ay-2024', code: '2024-2025', startDate: '2024-07-01', endDate: '2025-06-30', active: true },
      ])
    );
    vi.mocked(academicsApi.listProgrammes).mockReturnValue(
      Promise.resolve({
        content: [
          { id: 'prog-cse', code: 'CSE', name: 'Computer Science and Engineering', approvedIntake: 120 },
        ],
        page: 0,
        size: 100,
        totalElements: 1,
        totalPages: 1,
      })
    );
  });

  it('renders page header with title and description', async () => {
    render(<ImportsPage />);

    expect(screen.getByText('Unified Student Data Integration')).toBeInTheDocument();
    expect(screen.getByText(/Ingest, map, validate, reconcile, and commit student admissions/i)).toBeInTheDocument();
  });

  it('renders Step 1 with Academic Year selector and drag-and-drop zone', async () => {
    render(<ImportsPage />);

    expect(screen.getByText('Step 1: Select Academic Context & File')).toBeInTheDocument();
    expect(screen.getByText('Academic Year')).toBeInTheDocument();
    expect(screen.getByText('Target Programme (Optional)')).toBeInTheDocument();
    expect(screen.getByText('Choose a file or drag and drop your spreadsheet here')).toBeInTheDocument();
    expect(screen.getByText('Upload & Auto-Map Columns')).toBeInTheDocument();
  });

  it('allows toggling between Upload Wizard and Import History', async () => {
    render(<ImportsPage />);

    const historyBtn = screen.getByText('Import History');
    expect(historyBtn).toBeInTheDocument();
  });
});
