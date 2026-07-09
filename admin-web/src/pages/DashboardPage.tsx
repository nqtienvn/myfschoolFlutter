import { useEffect, useState } from 'react';
import { getDashboardStats, DashboardStats } from '../api/dashboard';

export default function DashboardPage({ selectedYearId, selectedSemesterId }: { selectedYearId?: string; selectedSemesterId?: string }) {
  const [stats, setStats] = useState<DashboardStats>({
    studentsCount: 1250,
    teachersCount: 85,
    classesCount: 45
  });
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'K10' | 'K11' | 'K12'>('K10');

  useEffect(() => {
    setLoading(true);
    getDashboardStats(selectedYearId)
      .then(res => {
        setStats(res);
      })
      .catch(err => {
        console.error('Failed to load stats', err);
      })
      .finally(() => {
        setLoading(false);
      });
  }, [selectedYearId, selectedSemesterId]);

  // Data for weekly attendance
  const weeklyAttendance = [
    { day: 'Thứ 2', rate: 96, height: 180 },
    { day: 'Thứ 3', rate: 94, height: 165 },
    { day: 'Thứ 4', rate: 98, height: 190 },
    { day: 'Thứ 5', rate: 95, height: 172 },
    { day: 'Thứ 6', rate: 91, height: 150 },
  ];

  // SVG Area Chart Data points depending on active tab
  const gpaData: Record<'K10' | 'K11' | 'K12', { month: string; gpa: number }[]> = {
    K10: [
      { month: 'Tháng 9', gpa: 6.8 },
      { month: 'Tháng 10', gpa: 7.2 },
      { month: 'Tháng 11', gpa: 7.0 },
      { month: 'Tháng 12', gpa: 7.5 },
      { month: 'Tháng 1', gpa: 7.3 },
      { month: 'Tháng 2', gpa: 7.8 },
      { month: 'Tháng 3', gpa: 8.2 },
      { month: 'Tháng 4', gpa: 8.0 },
      { month: 'Tháng 5', gpa: 8.4 },
    ],
    K11: [
      { month: 'Tháng 9', gpa: 7.2 },
      { month: 'Tháng 10', gpa: 7.5 },
      { month: 'Tháng 11', gpa: 7.3 },
      { month: 'Tháng 12', gpa: 7.9 },
      { month: 'Tháng 1', gpa: 7.6 },
      { month: 'Tháng 2', gpa: 8.1 },
      { month: 'Tháng 3', gpa: 8.4 },
      { month: 'Tháng 4', gpa: 8.3 },
      { month: 'Tháng 5', gpa: 8.7 },
    ],
    K12: [
      { month: 'Tháng 9', gpa: 7.5 },
      { month: 'Tháng 10', gpa: 7.8 },
      { month: 'Tháng 11', gpa: 7.7 },
      { month: 'Tháng 12', gpa: 8.2 },
      { month: 'Tháng 1', gpa: 8.0 },
      { month: 'Tháng 2', gpa: 8.5 },
      { month: 'Tháng 3', gpa: 8.9 },
      { month: 'Tháng 4', gpa: 8.8 },
      { month: 'Tháng 5', gpa: 9.2 },
    ]
  };

  const activeGpaPoints = gpaData[activeTab];

  // Convert GPA points to SVG coordinates
  // Width 800, Height 250
  // GPA maps 0-10 to Y coordinate 250-0
  const pointsString = activeGpaPoints
    .map((p, i) => `${(i * 100) + 50},${250 - (p.gpa * 22)}`)
    .join(' ');

  const fillPointsString = `50,250 ${pointsString} ${(activeGpaPoints.length - 1) * 100 + 50},250`;

  return (
    <div style={{ paddingBottom: '40px' }}>
      {/* 3 top Row Stats Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '24px', marginBottom: '24px' }}>
        {/* Card 1: Students */}
        <div className="db-card" style={{ padding: '24px', display: 'flex', flexDirection: 'column', position: 'relative' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <div style={{ width: '46px', height: '46px', borderRadius: '50%', background: 'var(--stamp-soft)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--stamp)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                <circle cx="9" cy="7" r="4"></circle>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
              </svg>
            </div>
          </div>
          <div style={{ fontSize: '14px', color: '#64748b', fontWeight: 500, marginBottom: '4px' }}>Tổng số học sinh</div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
            <span style={{ fontSize: '28px', fontWeight: 700, color: '#1c2434' }}>
              {loading ? '...' : stats.studentsCount.toLocaleString()}
            </span>
            <span style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '12px', color: '#10b981', fontWeight: 600, background: '#e1fbf2', padding: '4px 8px', borderRadius: '20px' }}>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3">
                <polyline points="18 15 12 9 6 15"></polyline>
              </svg>
              12.4%
            </span>
          </div>
        </div>

        {/* Card 2: Teachers */}
        <div className="db-card" style={{ padding: '24px', display: 'flex', flexDirection: 'column', position: 'relative' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <div style={{ width: '46px', height: '46px', borderRadius: '50%', background: 'var(--stamp-soft)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--stamp)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect>
                <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path>
              </svg>
            </div>
          </div>
          <div style={{ fontSize: '14px', color: '#64748b', fontWeight: 500, marginBottom: '4px' }}>Tổng số giáo viên</div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
            <span style={{ fontSize: '28px', fontWeight: 700, color: '#1c2434' }}>
              {loading ? '...' : stats.teachersCount.toLocaleString()}
            </span>
            <span style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '12px', color: '#10b981', fontWeight: 600, background: '#e1fbf2', padding: '4px 8px', borderRadius: '20px' }}>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3">
                <polyline points="18 15 12 9 6 15"></polyline>
              </svg>
              3.1%
            </span>
          </div>
        </div>

        {/* Card 3: Classes */}
        <div className="db-card" style={{ padding: '24px', display: 'flex', flexDirection: 'column', position: 'relative' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <div style={{ width: '46px', height: '46px', borderRadius: '50%', background: 'var(--stamp-soft)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--stamp)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
                <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
              </svg>
            </div>
          </div>
          <div style={{ fontSize: '14px', color: '#64748b', fontWeight: 500, marginBottom: '4px' }}>Tổng số lớp học</div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
            <span style={{ fontSize: '28px', fontWeight: 700, color: '#1c2434' }}>
              {loading ? '...' : stats.classesCount.toLocaleString()}
            </span>
            <span style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '12px', color: '#10b981', fontWeight: 600, background: '#e1fbf2', padding: '4px 8px', borderRadius: '20px' }}>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3">
                <polyline points="18 15 12 9 6 15"></polyline>
              </svg>
              5.8%
            </span>
          </div>
        </div>
      </div>

      {/* Middle Row Charts */}
      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '24px', marginBottom: '24px', flexWrap: 'wrap' }} className="db-mid-row">
        {/* Left Card: Weekly Attendance Bar Chart */}
        <div className="db-card" style={{ padding: '24px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
            <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 700, color: '#1c2434' }}>Chuyên cần hàng tuần</h3>
            <span style={{ fontSize: '12px', color: '#64748b', fontWeight: 500 }}>Tuần học hiện tại</span>
          </div>
          
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', height: '220px', padding: '0 20px', borderBottom: '1px solid #e2e8f0', position: 'relative' }}>
            {/* Background grid lines */}
            <div style={{ position: 'absolute', left: 0, right: 0, top: '25%', borderTop: '1px dashed #f1f5f9', height: 0 }}></div>
            <div style={{ position: 'absolute', left: 0, right: 0, top: '50%', borderTop: '1px dashed #f1f5f9', height: 0 }}></div>
            <div style={{ position: 'absolute', left: 0, right: 0, top: '75%', borderTop: '1px dashed #f1f5f9', height: 0 }}></div>

            {weeklyAttendance.map((item, idx) => (
              <div key={idx} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', width: '60px', zIndex: 2 }}>
                <div style={{ fontSize: '11px', fontWeight: 600, color: 'var(--stamp)', marginBottom: '8px' }}>{item.rate}%</div>
                <div style={{ 
                  width: '28px', 
                  height: `${item.height}px`, 
                  background: 'linear-gradient(180deg, var(--stamp) 0%, rgba(60, 80, 224, 0.4) 100%)', 
                  borderRadius: '6px 6px 0 0',
                  transition: 'height 0.3s ease-in-out',
                }} className="bar-hover"></div>
              </div>
            ))}
          </div>
          
          <div style={{ display: 'flex', justifyContent: 'space-between', padding: '12px 20px 0 20px' }}>
            {weeklyAttendance.map((item, idx) => (
              <div key={idx} style={{ width: '60px', textAlign: 'center', fontSize: '12px', fontWeight: 600, color: '#64748b' }}>
                {item.day}
              </div>
            ))}
          </div>
        </div>

        {/* Right Card: Circular Attendance Target */}
        <div className="db-card" style={{ padding: '24px', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ width: '100%', display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 700, color: '#1c2434' }}>Mục tiêu học kỳ</h3>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#64748b" strokeWidth="2">
              <circle cx="12" cy="12" r="1"></circle>
              <circle cx="12" cy="5" r="1"></circle>
              <circle cx="12" cy="19" r="1"></circle>
            </svg>
          </div>

          {/* SVG Circular Progress */}
          <div style={{ position: 'relative', width: '160px', height: '160px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <svg width="150" height="150" viewBox="0 0 100 100" style={{ transform: 'rotate(-90deg)' }}>
              {/* Background circle */}
              <circle cx="50" cy="50" r="40" stroke="#f1f5f9" strokeWidth="8" fill="transparent" />
              {/* Foreground circle */}
              <circle 
                cx="50" cy="50" r="40" 
                stroke="var(--stamp)" strokeWidth="8" fill="transparent" 
                strokeDasharray="251.2"
                strokeDashoffset={251.2 - (251.2 * 95.5) / 100}
                strokeLinecap="round"
              />
            </svg>
            <div style={{ position: 'absolute', textAlign: 'center' }}>
              <div style={{ fontSize: '26px', fontWeight: 700, color: '#1c2434' }}>95.55%</div>
              <div style={{ fontSize: '11px', color: '#10b981', fontWeight: 600 }}>+1.2% chuyên cần</div>
            </div>
          </div>

          <div style={{ width: '100%', textAlign: 'center', fontSize: '12px', color: '#64748b', padding: '10px 0' }}>
            Hôm nay tỉ lệ đến lớp cao hơn trung bình tuần trước. Keep it up!
          </div>

          {/* Details Row */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', width: '100%', borderTop: '1px solid #f1f5f9', paddingTop: '16px', gap: '8px' }}>
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: '11px', color: '#64748b', fontWeight: 500 }}>Có mặt</div>
              <div style={{ fontSize: '14px', fontWeight: 700, color: '#1c2434' }}>95.5%</div>
            </div>
            <div style={{ textAlign: 'center', borderLeft: '1px solid #f1f5f9', borderRight: '1px solid #f1f5f9' }}>
              <div style={{ fontSize: '11px', color: '#64748b', fontWeight: 500 }}>Đi muộn</div>
              <div style={{ fontSize: '14px', fontWeight: 700, color: '#f59e0b' }}>2.3%</div>
            </div>
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: '11px', color: '#64748b', fontWeight: 500 }}>Nghỉ học</div>
              <div style={{ fontSize: '14px', fontWeight: 700, color: '#ef4444' }}>2.2%</div>
            </div>
          </div>
        </div>
      </div>

      {/* Bottom Row Area Chart */}
      <div className="db-card" style={{ padding: '24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px', flexWrap: 'wrap', gap: '12px' }}>
          <div>
            <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 700, color: '#1c2434' }}>Tiến trình GPA & Học lực học sinh</h3>
            <span style={{ fontSize: '12px', color: '#64748b' }}>Đánh giá trung bình theo tháng học</span>
          </div>
          
          <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
            {/* Tabs */}
            <div style={{ display: 'flex', background: '#f1f5f9', padding: '3px', borderRadius: '6px' }}>
              {(['K10', 'K11', 'K12'] as const).map(tab => (
                <button
                  key={tab}
                  onClick={() => setActiveTab(tab)}
                  style={{
                    border: 'none',
                    background: activeTab === tab ? '#ffffff' : 'transparent',
                    color: activeTab === tab ? 'var(--stamp)' : '#64748b',
                    fontSize: '12px',
                    fontWeight: 600,
                    padding: '6px 16px',
                    borderRadius: '4px',
                    cursor: 'pointer',
                    boxShadow: activeTab === tab ? '0px 1px 3px rgba(0, 0, 0, 0.05)' : 'none'
                  }}
                >
                  {tab === 'K10' ? 'Khối 10' : tab === 'K11' ? 'Khối 11' : 'Khối 12'}
                </button>
              ))}
            </div>
            
            <div style={{ fontSize: '12px', fontWeight: 600, color: 'var(--stamp)', padding: '6px 12px', background: 'var(--stamp-soft)', borderRadius: '6px' }}>
              Niên khóa 2026-2027
            </div>
          </div>
        </div>

        {/* SVG Area Chart */}
        <div style={{ width: '100%', overflowX: 'auto' }}>
          <svg viewBox="0 0 900 280" width="100%" height="280" style={{ minWidth: '800px' }}>
            <defs>
              <linearGradient id="areaGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="var(--stamp)" stopOpacity="0.25" />
                <stop offset="100%" stopColor="var(--stamp)" stopOpacity="0.0" />
              </linearGradient>
            </defs>

            {/* Grid Lines */}
            <line x1="50" y1="30" x2="850" y2="30" stroke="#f1f5f9" strokeWidth="1" />
            <line x1="50" y1="85" x2="850" y2="85" stroke="#f1f5f9" strokeWidth="1" />
            <line x1="50" y1="140" x2="850" y2="140" stroke="#f1f5f9" strokeWidth="1" />
            <line x1="50" y1="195" x2="850" y2="195" stroke="#f1f5f9" strokeWidth="1" />
            <line x1="50" y1="250" x2="850" y2="250" stroke="#e2e8f0" strokeWidth="1.5" />

            {/* Y Axis Labels */}
            <text x="30" y="34" fill="#94a3b8" fontSize="11" fontWeight="600" textAnchor="end">10.0</text>
            <text x="30" y="89" fill="#94a3b8" fontSize="11" fontWeight="600" textAnchor="end">7.5</text>
            <text x="30" y="144" fill="#94a3b8" fontSize="11" fontWeight="600" textAnchor="end">5.0</text>
            <text x="30" y="199" fill="#94a3b8" fontSize="11" fontWeight="600" textAnchor="end">2.5</text>
            <text x="30" y="254" fill="#94a3b8" fontSize="11" fontWeight="600" textAnchor="end">0.0</text>

            {/* Gradient Area under line */}
            <polygon points={fillPointsString} fill="url(#areaGradient)" />

            {/* Stroke Line */}
            <polyline 
              fill="none" 
              stroke="var(--stamp)" 
              strokeWidth="3.5" 
              points={pointsString}
              strokeLinecap="round"
              strokeLinejoin="round"
            />

            {/* Interactive Circles / Tooltips */}
            {activeGpaPoints.map((p, i) => {
              const x = (i * 100) + 50;
              const y = 250 - (p.gpa * 22);
              return (
                <g key={i}>
                  <circle 
                    cx={x} cy={y} r="5" 
                    fill="#ffffff" stroke="var(--stamp)" strokeWidth="3"
                  />
                  {/* Monthly X Axis Label */}
                  <text x={x} y="272" fill="#64748b" fontSize="11" fontWeight="600" textAnchor="middle">
                    {p.month}
                  </text>
                  {/* Hover GPA Value */}
                  <text x={x} y={y - 12} fill="var(--stamp)" fontSize="11" fontWeight="700" textAnchor="middle">
                    {p.gpa}
                  </text>
                </g>
              );
            })}
          </svg>
        </div>
      </div>
    </div>
  );
}
