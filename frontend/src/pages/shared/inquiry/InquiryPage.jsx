import React, { useCallback, useEffect, useState } from 'react';
import {
  CheckCircle2,
  ChevronRight,
  CircleHelp,
  Clock3,
  LockKeyhole,
  Megaphone,
  MessageCircle,
  Plus,
  Send,
  ShieldCheck,
  Trash2,
  UserRound,
  X,
} from 'lucide-react';
import V3SiteHeader from '../../../components/layout/V3SiteHeader';
import { useSession } from '../../../contexts/SessionContext';
import './InquiryPage.css';

const API_BASE = `http://${window.location.hostname}:8080`;
const EMPTY_FORM = { title: '', content: '', secret: false, notice: false };

const formatDate = (value) => {
  if (!value) return '';
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
};

const getRoleLabel = (role) => {
  if (role === 'ROLE_ADMIN') return '관리자';
  if (role === 'ROLE_SELLER') return '판매자';
  return '구매자';
};

const readError = async (response, fallback) => {
  try {
    const body = await response.json();
    return body.message || body.error || fallback;
  } catch {
    return fallback;
  }
};

const InquiryPage = () => {
  const { session } = useSession();
  const isAdmin = session?.role === 'ROLE_ADMIN';
  const [inquiries, setInquiries] = useState([]);
  const [authorType, setAuthorType] = useState('ALL');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [composerOpen, setComposerOpen] = useState(false);
  const [form, setForm] = useState(EMPTY_FORM);
  const [submitting, setSubmitting] = useState(false);
  const [selected, setSelected] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [comment, setComment] = useState('');
  const [adminActionLoading, setAdminActionLoading] = useState(false);

  const loadInquiries = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const params = new URLSearchParams();
      if (isAdmin) params.set('authorType', authorType);
      const response = await fetch(`${API_BASE}/api/inquiries?${params}`, {
        credentials: 'include',
      });
      if (!response.ok) throw new Error(await readError(response, '문의 목록을 불러오지 못했습니다.'));
      setInquiries(await response.json());
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }, [authorType, isAdmin]);

  useEffect(() => {
    loadInquiries();
  }, [loadInquiries]);

  const openComposer = (notice = false) => {
    setError('');
    setForm({ ...EMPTY_FORM, notice });
    setComposerOpen(true);
  };

  const openDetail = async (id) => {
    setDetailLoading(true);
    setError('');
    try {
      const response = await fetch(`${API_BASE}/api/inquiries/${id}`, {
        credentials: 'include',
      });
      if (!response.ok) throw new Error(await readError(response, '문의 내용을 불러오지 못했습니다.'));
      setSelected(await response.json());
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setDetailLoading(false);
    }
  };

  const handleCreate = async (event) => {
    event.preventDefault();
    const title = form.title.trim();
    const content = form.content.trim();
    if (!title || !content) {
      setError('제목과 내용을 모두 입력해주세요.');
      return;
    }

    setSubmitting(true);
    setError('');
    try {
      const response = await fetch(`${API_BASE}/api/inquiries`, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          title,
          content,
          secret: form.notice ? false : form.secret,
          notice: isAdmin && form.notice,
        }),
      });
      if (!response.ok) throw new Error(await readError(response, '문의 등록에 실패했습니다.'));
      const created = await response.json();
      setForm(EMPTY_FORM);
      setComposerOpen(false);
      setSelected(created);
      await loadInquiries();
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSubmitting(false);
    }
  };

  const runAdminAction = async (url, options, fallback, refreshDetail = true) => {
    setAdminActionLoading(true);
    setError('');
    try {
      const response = await fetch(`${API_BASE}${url}`, {
        credentials: 'include',
        ...options,
      });
      if (!response.ok) throw new Error(await readError(response, fallback));
      await loadInquiries();
      if (selected && refreshDetail) await openDetail(selected.id);
      return true;
    } catch (requestError) {
      setError(requestError.message);
      return false;
    } finally {
      setAdminActionLoading(false);
    }
  };

  const handleComment = async (event) => {
    event.preventDefault();
    const content = comment.trim();
    if (!content) return;
    const success = await runAdminAction(
      `/api/admin/inquiries/${selected.id}/comments`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content }),
      },
      '답변 등록에 실패했습니다.',
    );
    if (success) setComment('');
  };

  const handleAnswered = () => runAdminAction(
    `/api/admin/inquiries/${selected.id}/answer`,
    {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ answered: !selected.answered }),
    },
    '답변 상태 변경에 실패했습니다.',
  );

  const handleDelete = async () => {
    if (!window.confirm('이 문의글을 강제로 삭제하시겠습니까? 댓글도 함께 삭제됩니다.')) return;
    const success = await runAdminAction(
      `/api/admin/inquiries/${selected.id}`,
      { method: 'DELETE' },
      '문의글 삭제에 실패했습니다.',
      false,
    );
    if (success) setSelected(null);
  };

  return (
    <div className="inquiry-page">
      <V3SiteHeader />

      <main className="inquiry-main">
        <header className="inquiry-page-heading">
          <h1><CircleHelp size={28} aria-hidden="true" />문의사항</h1>
          <div className="inquiry-heading-actions">
            {isAdmin && (
              <button className="inquiry-notice-button" type="button" onClick={() => openComposer(true)}>
                <Megaphone size={18} /> 전체 공지 작성
              </button>
            )}
            <button className="inquiry-primary-button" type="button" onClick={() => openComposer(false)}>
              <Plus size={18} /> 문의 작성
            </button>
          </div>
        </header>

        <section className="inquiry-board" aria-label="문의사항 목록">
          {isAdmin && (
            <div className="inquiry-toolbar">
              <div className="inquiry-filter" aria-label="작성자 유형 필터">
                {[
                  ['ALL', '모두 보기'],
                  ['SELLER', '판매자만'],
                  ['BUYER', '구매자만'],
                ].map(([value, label]) => (
                  <button
                    type="button"
                    key={value}
                    className={authorType === value ? 'is-active' : ''}
                    onClick={() => setAuthorType(value)}
                  >
                    {label}
                  </button>
                ))}
              </div>
            </div>
          )}

          {error && <div className="inquiry-error" role="alert">{error}</div>}

          {loading ? (
            <div className="inquiry-empty">문의 목록을 불러오는 중입니다.</div>
          ) : inquiries.length === 0 ? (
            <div className="inquiry-empty">
              <MessageCircle size={30} />
              <strong>등록된 문의가 없습니다.</strong>
              <span>첫 문의를 작성해보세요.</span>
            </div>
          ) : (
            <div className="inquiry-list">
              {inquiries.map((item) => (
                <button type="button" className={`inquiry-row ${item.notice ? 'is-notice' : ''}`} key={item.id} onClick={() => openDetail(item.id)}>
                  {item.notice ? (
                    <span className="inquiry-status is-notice"><Megaphone size={15} />공지</span>
                  ) : (
                    <span className={`inquiry-status ${item.answered ? 'is-done' : 'is-waiting'}`}>
                      {item.answered ? <CheckCircle2 size={15} /> : <Clock3 size={15} />}
                      {item.answered ? '답변 완료' : '답변 미완료'}
                    </span>
                  )}
                  <span className="inquiry-row-main">
                    <strong>
                      {item.secret && !item.notice && <LockKeyhole size={15} aria-label="비밀글" />}
                      {item.title}
                    </strong>
                    <small>
                      <span className={`inquiry-role role-${getRoleLabel(item.authorRole)}`}>
                        {getRoleLabel(item.authorRole)}
                      </span>
                      {item.authorNickname} · {formatDate(item.createdAt)}
                      {item.commentCount > 0 && ` · 답변 ${item.commentCount}`}
                    </small>
                  </span>
                  <ChevronRight size={20} />
                </button>
              ))}
            </div>
          )}
        </section>
      </main>

      {composerOpen && (
        <div className="inquiry-modal-backdrop" role="presentation" onMouseDown={() => setComposerOpen(false)}>
          <section className="inquiry-modal inquiry-compose-modal" role="dialog" aria-modal="true" aria-labelledby="compose-title" onMouseDown={(event) => event.stopPropagation()}>
            <div className="inquiry-modal-header">
              <div>
                <span>{form.notice ? 'GLOBAL NOTICE' : 'NEW INQUIRY'}</span>
                <h2 id="compose-title">{form.notice ? '전체 공지 작성' : '문의 작성'}</h2>
              </div>
              <button type="button" aria-label="닫기" onClick={() => setComposerOpen(false)}><X /></button>
            </div>
            <form onSubmit={handleCreate}>
              {error && <div className="inquiry-error" role="alert">{error}</div>}
              <label>
                <span>제목</span>
                <input
                  maxLength={100}
                  value={form.title}
                  onChange={(event) => setForm({ ...form, title: event.target.value })}
                  placeholder={form.notice ? '공지 제목을 입력해주세요' : '문의 제목을 입력해주세요'}
                  autoFocus
                />
                <small>{form.title.length}/100</small>
              </label>
              <label>
                <span>내용</span>
                <textarea
                  maxLength={5000}
                  rows={9}
                  value={form.content}
                  onChange={(event) => setForm({ ...form, content: event.target.value })}
                  placeholder={form.notice ? '모든 사용자에게 전달할 내용을 입력해주세요' : '문의 내용을 구체적으로 작성해주세요'}
                />
                <small>{form.content.length}/5000</small>
              </label>
              {isAdmin && (
                <label className="inquiry-notice-option">
                  <input
                    type="checkbox"
                    checked={form.notice}
                    onChange={(event) => setForm({
                      ...form,
                      notice: event.target.checked,
                      secret: event.target.checked ? false : form.secret,
                    })}
                  />
                  <span><Megaphone size={17} /><strong>전체 공지로 등록</strong><small>목록 최상단에 고정되며 모든 사용자가 볼 수 있습니다.</small></span>
                </label>
              )}
              {!form.notice && <label className="inquiry-secret-option">
                <input
                  type="checkbox"
                  checked={form.secret}
                  onChange={(event) => setForm({ ...form, secret: event.target.checked })}
                />
                <span><LockKeyhole size={17} /><strong>비밀글로 작성</strong><small>작성자와 관리자만 볼 수 있습니다.</small></span>
              </label>}
              <div className="inquiry-form-actions">
                <button type="button" className="inquiry-secondary-button" onClick={() => setComposerOpen(false)}>취소</button>
                <button type="submit" className="inquiry-primary-button" disabled={submitting}>
                  <Send size={17} /> {submitting ? '등록 중...' : form.notice ? '공지 등록' : '문의 등록'}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}

      {(selected || detailLoading) && (
        <div className="inquiry-modal-backdrop" role="presentation" onMouseDown={() => !adminActionLoading && setSelected(null)}>
          <section className="inquiry-modal inquiry-detail-modal" role="dialog" aria-modal="true" aria-labelledby="detail-title" onMouseDown={(event) => event.stopPropagation()}>
            {detailLoading && !selected ? (
              <div className="inquiry-empty">문의 내용을 불러오는 중입니다.</div>
            ) : selected && (
              <>
                <div className="inquiry-modal-header">
                  <div>
                    <span>{selected.notice ? 'GLOBAL NOTICE' : selected.secret ? 'PRIVATE INQUIRY' : 'PUBLIC INQUIRY'}</span>
                    <h2 id="detail-title">{selected.title}</h2>
                  </div>
                  <button type="button" aria-label="닫기" onClick={() => setSelected(null)}><X /></button>
                </div>

                <div className="inquiry-detail-meta">
                  {selected.notice ? (
                    <span className="inquiry-status is-notice"><Megaphone size={15} />공지</span>
                  ) : (
                    <span className={`inquiry-status ${selected.answered ? 'is-done' : 'is-waiting'}`}>
                      {selected.answered ? <CheckCircle2 size={15} /> : <Clock3 size={15} />}
                      {selected.answered ? '답변 완료' : '답변 미완료'}
                    </span>
                  )}
                  <span><UserRound size={15} /> {getRoleLabel(selected.authorRole)} · {selected.authorNickname}</span>
                  <time>{formatDate(selected.createdAt)}</time>
                </div>

                {error && <div className="inquiry-error" role="alert">{error}</div>}

                <article className="inquiry-content">{selected.content}</article>

                {!selected.notice && <section className="inquiry-comments">
                  <div className="inquiry-comments-title">
                    <ShieldCheck size={19} /><strong>관리자 답변</strong><span>{selected.comments.length}</span>
                  </div>
                  {selected.comments.length === 0 ? (
                    <p className="inquiry-no-comment">아직 등록된 관리자 답변이 없습니다.</p>
                  ) : selected.comments.map((item) => (
                    <article className="inquiry-comment" key={item.id}>
                      <header><strong>{item.adminNickname} 관리자</strong><time>{formatDate(item.createdAt)}</time></header>
                      <p>{item.content}</p>
                    </article>
                  ))}
                </section>}

                {isAdmin && (
                  <section className="inquiry-admin-panel">
                    {!selected.notice && <form onSubmit={handleComment}>
                      <label htmlFor="admin-comment">관리자 답변 작성</label>
                      <div>
                        <textarea
                          id="admin-comment"
                          rows={3}
                          maxLength={2000}
                          value={comment}
                          onChange={(event) => setComment(event.target.value)}
                          placeholder="문의자에게 전달할 답변을 입력하세요"
                        />
                        <button type="submit" disabled={adminActionLoading || !comment.trim()}><Send size={17} /> 답변 등록</button>
                      </div>
                    </form>}
                    <div className="inquiry-admin-actions">
                      {!selected.notice && <button type="button" className="inquiry-answer-toggle" disabled={adminActionLoading} onClick={handleAnswered}>
                        {selected.answered ? <Clock3 size={17} /> : <CheckCircle2 size={17} />}
                        {selected.answered ? '미완료로 변경' : '답변 완료 처리'}
                      </button>}
                      <button type="button" className="inquiry-delete-button" disabled={adminActionLoading} onClick={handleDelete}>
                        <Trash2 size={17} /> 강제 삭제
                      </button>
                    </div>
                  </section>
                )}
              </>
            )}
          </section>
        </div>
      )}
    </div>
  );
};

export default InquiryPage;
