import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { Button } from '@/components/ui/Button';

describe('Button component', () => {
  it('renders button with label and handles click', () => {
    const handleClick = vi.fn();
    render(<Button onClick={handleClick}>Submit Data</Button>);
    const button = screen.getByRole('button', { name: /Submit Data/i });
    expect(button).toBeInTheDocument();
    fireEvent.click(button);
    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it('respects disabled state', () => {
    const handleClick = vi.fn();
    render(<Button onClick={handleClick} disabled>Disabled Action</Button>);
    const button = screen.getByRole('button', { name: /Disabled Action/i });
    expect(button).toBeDisabled();
    fireEvent.click(button);
    expect(handleClick).not.toHaveBeenCalled();
  });
});

