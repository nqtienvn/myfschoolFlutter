export interface ShiftSelectorItem {
  id: number;
  name: string;
  code?: string;
}

export interface PeriodSelectorItem {
  id: number;
  name: string;
  shiftId: number;
}

interface Props {
  shifts: ShiftSelectorItem[];
  periods: PeriodSelectorItem[];
  selectedShiftIds: number[];
  selectedPeriodIds: number[];
  onToggleShift: (id: number) => void;
  onTogglePeriod: (id: number) => void;
  disabled?: boolean;
}

export default function ShiftPeriodSelector({
  shifts,
  periods,
  selectedShiftIds,
  selectedPeriodIds,
  onToggleShift,
  onTogglePeriod,
  disabled = false,
}: Props) {
  return <div className="shift-period-selector">
    {shifts.map(shift => {
      const selected = selectedShiftIds.includes(shift.id);
      const shiftPeriods = periods.filter(period => period.shiftId === shift.id);
      return <div key={shift.id} className={`shift-period-group ${selected ? 'selected' : ''}`}>
        <label className="shift-period-header">
          <input type="checkbox" checked={selected} disabled={disabled} onChange={() => onToggleShift(shift.id)} />
          <span className="shift-period-title">{shift.name}{shift.code ? ` (${shift.code})` : ''}</span>
          <span className={`badge-status ${selected ? 'active' : ''}`}>{selected ? 'ĐANG ÁP DỤNG' : 'CHƯA ÁP DỤNG'}</span>
        </label>
        {selected && <div className="shift-period-options">
          {shiftPeriods.map(period => {
            const periodSelected = selectedPeriodIds.includes(period.id);
            return <label key={period.id} className={`shift-period-option ${periodSelected ? 'selected' : ''}`}>
              <input type="checkbox" checked={periodSelected} disabled={disabled} onChange={() => onTogglePeriod(period.id)} />
              <span>{period.name}</span>
            </label>;
          })}
          {shiftPeriods.length === 0 && <span className="input-desc shift-period-empty">Không có tiết học nào.</span>}
        </div>}
      </div>;
    })}
    {shifts.length === 0 && <span className="input-desc">Chưa có ca học.</span>}
  </div>;
}
