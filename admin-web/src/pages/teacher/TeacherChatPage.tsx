import { useEffect, useState, type FormEvent } from 'react';
import {
  createConversation,
  getConversationMessages,
  getTeacherConversations,
  searchChatUsers,
  sendConversationMessage,
} from '../../api/teacher';
import { useTeacherAcademic } from '../../teacher/TeacherAcademicContext';

export default function TeacherChatPage() {
  const { selectedYearId } = useTeacherAcademic();
  const [conversations, setConversations] = useState<any[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [messages, setMessages] = useState<any[]>([]);
  const [text, setText] = useState('');
  const [search, setSearch] = useState('');
  const [searchRows, setSearchRows] = useState<any[]>([]);
  const [error, setError] = useState('');

  async function loadConversations() {
    try { setConversations(await getTeacherConversations() || []); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Không tải được tin nhắn.'); }
  }
  async function loadMessages(id: number) {
    try { setSelectedId(id); setMessages(await getConversationMessages(id) || []); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Không tải được hội thoại.'); }
  }
  useEffect(() => { setConversations([]); setSelectedId(null); setMessages([]); setSearchRows([]); void loadConversations(); }, [selectedYearId]);
  useEffect(() => {
    if (!selectedId) return;
    const timer = window.setInterval(() => { void loadMessages(selectedId); }, 5000);
    return () => clearInterval(timer);
  }, [selectedId]);

  async function send(event: FormEvent) {
    event.preventDefault(); if (!selectedId || !text.trim()) return;
    try { await sendConversationMessage(selectedId, text.trim()); setText(''); await loadMessages(selectedId); await loadConversations(); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Không gửi được tin nhắn.'); }
  }
  async function findUsers() {
    if (search.trim().length < 2) return;
    try { setSearchRows(await searchChatUsers(search.trim()) || []); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Không tìm được người dùng.'); }
  }
  async function startChat(userId: number) {
    try { const conversation = await createConversation(userId); setSearch(''); setSearchRows([]); await loadConversations(); await loadMessages(conversation.id); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Không tạo được hội thoại.'); }
  }

  const selected = conversations.find(item => item.id === selectedId);
  return <main className="teacher-page teacher-chat-page"><div className="page-heading"><div><span className="eyebrow">Trao đổi trực tiếp</span><h1>Tin nhắn</h1><p>Nhắn tin với phụ huynh và học sinh thuộc phạm vi giảng dạy.</p></div></div>{error && <div className="notice error">{error}</div>}<div className="teacher-chat-shell"><aside><div className="chat-search"><input value={search} onChange={event => setSearch(event.target.value)} onKeyDown={event => { if (event.key === 'Enter') void findUsers(); }} placeholder="Tìm phụ huynh/học sinh"/><button onClick={findUsers}>Tìm</button></div>{searchRows.map(user => <button className="chat-search-result" key={user.id} onClick={() => startChat(user.id)}><strong>{user.name}</strong><small>{user.role} · {user.phone}</small></button>)}<div className="chat-conversation-list">{conversations.map(item => <button className={selectedId === item.id ? 'active' : ''} key={item.id} onClick={() => loadMessages(item.id)}><span><strong>{item.otherParticipant?.name || 'Người dùng'}</strong><small>{item.lastMessage || 'Chưa có tin nhắn'}</small></span>{item.unreadCount > 0 && <b>{item.unreadCount}</b>}</button>)}</div></aside><section>{selected ? <><header><strong>{selected.otherParticipant?.name}</strong><small>{selected.otherParticipant?.role}</small></header><div className="chat-message-list">{messages.map(item => <article className={item.isMine ? 'mine' : ''} key={item.id || item.clientMessageId}><span>{item.content}</span><small>{new Date(item.createdAt).toLocaleString('vi-VN')}</small></article>)}</div><form onSubmit={send}><textarea rows={2} value={text} onChange={event => setText(event.target.value)} placeholder="Nhập tin nhắn…"/><button disabled={!text.trim()}>Gửi</button></form></> : <div className="empty-state">Chọn một hội thoại để bắt đầu.</div>}</section></div></main>;
}
