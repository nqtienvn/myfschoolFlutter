import { useState } from 'react';
import { createAnnouncement } from '../api/announcement';

export default function AnnouncementsPage() {
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const [targetRole, setTargetRole] = useState('ALL');
  const [classIds, setClassIds] = useState('');

  async function handleCreateAnnouncement() {
    try {
      await createAnnouncement({
        title,
        body,
        targetRole,
        requiresReply: false,
        classIds: classIds.split(',').map(Number).filter(Boolean),
      });
      alert('Tạo thông báo thành công');
      setTitle(''); setBody('');
    } catch (err: any) { alert(err.message); }
  }

  return (
    <div>
      <h2>Thông báo</h2>
      <div className="form-inline" style={{ flexDirection: 'column', alignItems: 'flex-start', gap: 12 }}>
        <input placeholder="Tiêu đề" value={title} onChange={e => setTitle(e.target.value)} style={{ width: 400 }} />
        <textarea placeholder="Nội dung" value={body} onChange={e => setBody(e.target.value)} rows={4} style={{ width: 400 }} />
        <div className="form-inline">
          <select value={targetRole} onChange={e => setTargetRole(e.target.value)}>
            <option value="ALL">Tất cả</option>
            <option value="PARENT">Phụ huynh</option>
            <option value="STUDENT">Học sinh</option>
          </select>
          <input placeholder="Class IDs (phẩy)" value={classIds} onChange={e => setClassIds(e.target.value)} />
          <button onClick={handleCreateAnnouncement}>Gửi thông báo</button>
        </div>
      </div>
    </div>
  );
}
