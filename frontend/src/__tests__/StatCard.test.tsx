import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { StatCard } from '@/components/ui/StatCard';

describe('StatCard component', () => {
  it('renders label and formatted value', () => {
    render(<StatCard label="Total Students" value={12500} hint="Active enrolments" tone="default" />);
    expect(screen.getByText('Total Students')).toBeInTheDocument();
    expect(screen.getByText('12,500')).toBeInTheDocument();
    expect(screen.getByText('Active enrolments')).toBeInTheDocument();
  });

  it('renders string values directly', () => {
    render(<StatCard label="Verified Rate" value="95%" tone="success" />);
    expect(screen.getByText('Verified Rate')).toBeInTheDocument();
    expect(screen.getByText('95%')).toBeInTheDocument();
  });
});

