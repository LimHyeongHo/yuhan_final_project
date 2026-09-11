import React, { useEffect, useMemo, useState } from 'react';
import {
  ArrowRight,
  BarChart3,
  Bell,
  BookOpen,
  Bookmark,
  ClipboardList,
  Home,
  Info,
  LayoutDashboard,
  LogOut,
  Menu,
  MessageCircle,
  Package,
  Plus,
  Search,
  Shield,
  ShoppingBag,
  Star,
  Timer,
  Users,
  X,
} from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { useChatNotifications } from '../../contexts/ChatNotificationContext';
import './homePage_v3.css';

const formatPrice = (value) => `${Number(value || 0).toLocaleString()}원`;
const toTimestamp = (value) => new Date(String(value || '').replace(' ', 'T')).getTime() || 0;
const getShortTitle = (title) => String(title || '').split(/\s+-\s+/)[0].trim();

const mapProduct = (item) => {
  const current = Number(item.currentCount || 0);
  const target = Number(item.targetCount || 1);
  const deadline = new Date(item.deadline);
  const diffDays = Math.ceil((deadline - new Date()) / (1000 * 60 * 60 * 24));

  return {
    id: item.productId,
    title: getShortTitle(item.title),
    rawType: item.type,
    category: item.type === 'BOOK' ? '전공 도서' : '학과 상품',
    author: item.author || '판매자 정보 없음',
    current,
    target,
    originalPrice: formatPrice(item.originalPrice || item.price),
    price: formatPrice(item.price),
    dDay: diffDays > 0 ? `D-${diffDays}` : 'D-DAY',
    diffDays,
    progress: Math.min(Math.round((current / target) * 100), 100),
    thumbnail: item.imageUrl || null,
  };
};

const ProductCover = ({ product, variant = 'book' }) => (
  <div className={`v3-product-cover is-${variant}`}>
    {product?.thumbnail ? (
      <img src={product.thumbnail} alt={`${product.title} 이미지`} />
    ) : product ? (
      <div className="v3-faux-cover">
        {product.rawType !== 'BOOK' && <span>{product.category}</span>}
        {product.rawType === 'BOOK' ? <BookOpen size={27} /> : <ShoppingBag size={27} />}
        <strong>{product.title}</strong>
        <small>{product.author}</small>
      </div>
    ) : (
      <div className="v3-faux-cover"><BookOpen size={27} /><strong>YU BOOK</strong></div>
    )}
  </div>
);

const HeaderNav = ({ userRole, unreadCount, closeMenu }) => {
  if (userRole === 'ROLE_ADMIN') {
    return (
      <>
        <Link aria-label="관리자 홈" title="관리자 홈" to="/admin/dashboard" onClick={closeMenu}>
          <LayoutDashboard size={18} /><span>관리자 홈</span>
        </Link>
        <Link aria-label="회원 관리" title="회원 관리" to="/admin/authorization" onClick={closeMenu}>
          <Users size={18} /><span>회원 관리</span>
        </Link>
        <Link aria-label="상품 관리" title="상품 관리" to="/admin/products" onClick={closeMenu}>
          <Package size={18} /><span>상품 관리</span>
        </Link>
        <Link aria-label="보안 로그" title="보안 로그" to="/admin/security" onClick={closeMenu}>
          <Shield size={18} /><span>보안 로그</span>
        </Link>
      </>
    );
  }

  if (userRole === 'ROLE_SELLER') {
    return (
      <>
        <Link aria-label="판매자 대시보드" title="대시보드" to="/seller/dashboard" onClick={closeMenu}>
          <LayoutDashboard size={18} /><span>대시보드</span>
        </Link>
        <Link aria-label="물품 등록" title="물품 등록" to="/seller/products" onClick={closeMenu}>
          <Plus size={18} /><span>물품 등록</span>
        </Link>
        <Link aria-label="판매 현황" title="판매 현황" to="/seller/status" onClick={closeMenu}>
          <ClipboardList size={18} /><span>판매 현황</span>
        </Link>
        <Link aria-label="분석 데이터" title="분석 데이터" to="/seller/analytics" onClick={closeMenu}>
          <BarChart3 size={18} /><span>분석 데이터</span>
        </Link>
        <Link aria-label="채팅" title="채팅" className="v3-nav-chat" to="/seller/chat" onClick={closeMenu}>
          <MessageCircle size={18} /><span>채팅</span>{unreadCount > 0 && <i />}
        </Link>
      </>
    );
  }

  return (
    <>
      <Link aria-label="홈" title="홈" to="/home-v3" onClick={closeMenu}>
        <Home size={18} /><span>홈</span>
      </Link>
      <Link aria-label="공구찾기" title="공구찾기" to="/buyer/products" onClick={closeMenu}>
        <Search size={18} /><span>공구찾기</span>
      </Link>
      <Link aria-label="채팅" title="채팅" className="v3-nav-chat" to="/buyer/chat" onClick={closeMenu}>
        <MessageCircle size={18} /><span>채팅</span>{unreadCount > 0 && <i />}
      </Link>
      <Link aria-label="이용가이드" title="이용가이드" to="/guide" onClick={closeMenu}>
        <Info size={18} /><span>이용가이드</span>
      </Link>
    </>
  );
};

const IntegratedHeader = () => {
  const navigate = useNavigate();
  const { chatRooms } = useChatNotifications();
  const [mobileOpen, setMobileOpen] = useState(false);
  const nickname = localStorage.getItem('user_nickname');
  const userRole = localStorage.getItem('user_role') || 'ROLE_BUYER';
  const unreadCount = chatRooms.reduce((sum, room) => sum + Number(room.unreadCount || 0), 0);
  const notificationSeenKey = `v3_notification_seen_${localStorage.getItem('email') || 'guest'}`;
  const [participationAlerts, setParticipationAlerts] = useState([]);
  const [systemNotifications, setSystemNotifications] = useState([]);
  const [adminSecurityAlerts, setAdminSecurityAlerts] = useState([]);
  const [notificationOpen, setNotificationOpen] = useState(false);
  const [lastSeenAt, setLastSeenAt] = useState(
    () => localStorage.getItem(notificationSeenKey) || new Date(0).toISOString(),
  );
  const myPagePath = userRole === 'ROLE_SELLER'
    ? '/seller/mypage'
    : userRole === 'ROLE_ADMIN' ? '/admin/dashboard' : '/buyer/mypage';
  const savedItemsPath = userRole === 'ROLE_SELLER'
    ? '/seller/mypage/projects'
    : userRole === 'ROLE_ADMIN' ? '/admin/products' : '/buyer/mypage/scrap';
  const ordersPath = userRole === 'ROLE_SELLER'
    ? '/seller/orders'
    : userRole === 'ROLE_ADMIN' ? '/admin/settlements' : '/buyer/mypage/orders';
  const initials = nickname ? nickname.slice(0, 2).toUpperCase() : 'YU';

  useEffect(() => {
    if (!nickname) {
      setParticipationAlerts([]);
      setSystemNotifications([]);
      setAdminSecurityAlerts([]);
      return undefined;
    }

    let active = true;
    const apiBase = `http://${window.location.hostname}:8080/api`;

    const loadNotifications = async () => {
      try {
        const response = await fetch(`${apiBase}/seller/notifications`, { credentials: 'include' });
        if (response.ok && active) setSystemNotifications(await response.json());
      } catch (error) {
        console.error('개인 알림 로드 실패', error);
      }

      if (userRole === 'ROLE_SELLER') {
        try {
          const response = await fetch(`${apiBase}/products/seller/me/participations`, { credentials: 'include' });
          if (response.ok && active) setParticipationAlerts(await response.json());
        } catch (error) {
          console.error('판매 참여 알림 로드 실패', error);
        }
      } else if (active) {
        setParticipationAlerts([]);
      }

      if (userRole === 'ROLE_ADMIN') {
        try {
          const response = await fetch(`${apiBase}/admin/logs?type=SECURITY`, { credentials: 'include' });
          if (response.ok && active) {
            const logs = await response.json();
            setAdminSecurityAlerts(
              Array.isArray(logs)
                ? logs.filter((log) => log.status === 'TAMPERED' || log.status === 'FORGED')
                : [],
            );
          }
        } catch (error) {
          console.error('관리자 보안 알림 로드 실패', error);
        }
      } else if (active) {
        setAdminSecurityAlerts([]);
      }
    };

    loadNotifications();
    const intervalId = window.setInterval(loadNotifications, 20000);
    return () => {
      active = false;
      window.clearInterval(intervalId);
    };
  }, [nickname, userRole]);

  const lastSeenTimestamp = toTimestamp(lastSeenAt);
  const chatNotificationItems = chatRooms
    .filter((room) => Number(room.unreadCount || 0) > 0)
    .map((room) => ({
      type: 'CHAT',
      key: `chat-${room.roomId}`,
      roomId: room.roomId,
      title: room.targetName || '새 채팅 메시지',
      detail: room.lastMessage || room.productName || '새 메시지가 도착했습니다.',
      time: room.lastSentAt,
      count: Number(room.unreadCount || 0),
    }));
  const participationNotificationItems = participationAlerts
    .filter((item) => toTimestamp(item.joinDate) > lastSeenTimestamp)
    .map((item) => ({
      type: 'PARTICIPATION',
      key: `participation-${item.id}`,
      title: `${item.buyerName || '구매자'}님이 공구에 참여했습니다.`,
      detail: item.product?.title || '판매 상품 참여 알림',
      time: item.joinDate,
      count: 1,
    }));
  const systemNotificationItems = systemNotifications
    .filter((item) => toTimestamp(item.createdAt) > lastSeenTimestamp)
    .map((item) => ({
      type: 'SYSTEM',
      key: `system-${item.id}`,
      title: '시스템 알림',
      detail: item.message,
      time: item.createdAt,
      count: 1,
    }));
  const adminNotificationItems = adminSecurityAlerts
    .filter((item) => toTimestamp(item.timestamp) > lastSeenTimestamp)
    .map((item) => ({
      type: 'SECURITY',
      key: `security-${item.id}`,
      title: '보안 위변조 감지',
      detail: item.detail || item.diff || item.displayId,
      time: item.timestamp,
      count: 1,
    }));
  const notificationItems = [
    ...chatNotificationItems,
    ...participationNotificationItems,
    ...systemNotificationItems,
    ...adminNotificationItems,
  ].sort((a, b) => toTimestamp(b.time) - toTimestamp(a.time));
  const totalNotificationCount = notificationItems.reduce((sum, item) => sum + item.count, 0);

  const markNotificationsSeen = () => {
    const now = new Date().toISOString();
    localStorage.setItem(notificationSeenKey, now);
    setLastSeenAt(now);
  };

  const toggleNotifications = () => {
    if (notificationOpen) markNotificationsSeen();
    setNotificationOpen((open) => !open);
  };

  const openNotification = (item) => {
    markNotificationsSeen();
    setNotificationOpen(false);
    if (item.type === 'CHAT') {
      navigate(`${userRole === 'ROLE_SELLER' ? '/seller/chat' : '/buyer/chat'}?roomId=${item.roomId}`);
    } else if (item.type === 'PARTICIPATION') {
      navigate('/seller/status');
    } else if (item.type === 'SECURITY') {
      navigate('/admin/security');
    } else {
      navigate(userRole === 'ROLE_ADMIN' ? '/admin/dashboard' : userRole === 'ROLE_SELLER' ? '/seller/dashboard' : '/buyer/mypage');
    }
  };

  const handleLogout = async () => {
    try {
      await fetch(`http://${window.location.hostname}:8080/api/pki/logout`, {
        method: 'POST',
        credentials: 'include',
      });
    } catch (error) {
      console.error('서버 로그아웃 요청 실패', error);
    }

    localStorage.removeItem('user_nickname');
    localStorage.removeItem('user_role');
    localStorage.removeItem('email');
    navigate('/login');
  };

  return (
    <header className="v3-integrated-header">
      <Link className="v3-brand" to="/home-v3">
        <strong>N-bbang</strong>
      </Link>

      <nav className={mobileOpen ? 'is-open' : ''}>
        <HeaderNav
          userRole={userRole}
          unreadCount={unreadCount}
          closeMenu={() => setMobileOpen(false)}
        />
      </nav>

      <div className="v3-header-actions">
        {nickname ? (
          <>
            <div className="v3-notification-wrap">
              <button
                aria-expanded={notificationOpen}
                aria-label={`알림 ${totalNotificationCount}개`}
                className="v3-notification-button"
                onClick={toggleNotifications}
                title="알림"
                type="button"
              >
                <Bell size={19} />
                {totalNotificationCount > 0 && (
                  <span>{totalNotificationCount > 99 ? '99+' : totalNotificationCount}</span>
                )}
              </button>
              {notificationOpen && (
                <div className="v3-notification-panel">
                  <div className="v3-notification-heading">
                    <strong>알림</strong><span>{totalNotificationCount}개</span>
                  </div>
                  <div className="v3-notification-list">
                    {notificationItems.length > 0 ? notificationItems.slice(0, 6).map((item) => (
                      <button key={item.key} onClick={() => openNotification(item)} type="button">
                        <strong>{item.title}</strong>
                        <span>{item.detail}</span>
                        {item.count > 1 && <i>{item.count}</i>}
                      </button>
                    )) : (
                      <p>새로운 알림이 없습니다.</p>
                    )}
                  </div>
                  <button className="v3-notification-read" onClick={() => { markNotificationsSeen(); setNotificationOpen(false); }} type="button">
                    모두 확인
                  </button>
                </div>
              )}
            </div>
            {userRole === 'ROLE_BUYER' && (
              <>
                <Link aria-label="저장한 상품" className="v3-round-action" to={savedItemsPath}>
                  <Bookmark size={19} />
                </Link>
                <Link aria-label="주문 내역" className="v3-round-action" to={ordersPath}>
                  <ShoppingBag size={19} />
                </Link>
              </>
            )}
            <Link className="v3-profile" to={myPagePath}>
              <i>{initials}</i><span>{nickname} 님</span>
            </Link>
            <button aria-label="로그아웃" className="v3-round-action" onClick={handleLogout} type="button">
              <LogOut size={18} />
            </button>
          </>
        ) : (
          <div className="v3-auth-links">
            <Link to="/login">로그인</Link>
            <Link to="/signup">회원가입</Link>
          </div>
        )}
        <button
          aria-label={mobileOpen ? '메뉴 닫기' : '메뉴 열기'}
          className="v3-mobile-toggle"
          onClick={() => setMobileOpen((open) => !open)}
          type="button"
        >
          {mobileOpen ? <X size={21} /> : <Menu size={21} />}
        </button>
      </div>
    </header>
  );
};

const FeaturedProduct = ({ product, isFlipped, onFlip, onOpen }) => {
  if (!product) return <div className="v3-feature-empty">추천 상품을 준비하고 있어요.</div>;

  const handleBackKeyDown = (event) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      onFlip();
    }
  };

  return (
    <div className="v3-featured-product">
      <div className="v3-feature-shadow" />
      <div className={`v3-flip-card ${isFlipped ? 'is-flipped' : ''}`}>
        <button
          aria-label={`${product.title} 정보 보기`}
          className="v3-flip-face is-front"
          onClick={onFlip}
          tabIndex={isFlipped ? -1 : 0}
          type="button"
        >
          <ProductCover product={product} variant="featured" />
        </button>
        <div
          aria-hidden={!isFlipped}
          aria-label={`${product.title} 표지 다시 보기`}
          className="v3-flip-face is-back"
          onClick={onFlip}
          onKeyDown={handleBackKeyDown}
          role="button"
          tabIndex={isFlipped ? 0 : -1}
        >
          <span>{product.category} · {product.dDay}</span>
          <BookOpen size={31} />
          <strong>{product.title}</strong>
          <small>{product.author}</small>
          <div className="v3-flip-price">
            <del>{product.originalPrice}</del>
            <b>{product.price}</b>
          </div>
          <div className="v3-flip-progress"><i style={{ width: `${product.progress}%` }} /></div>
          <p>{product.current}/{product.target}명 · 달성률 {product.progress}%</p>
          <button
            className="v3-product-detail-button"
            onClick={(event) => { event.stopPropagation(); onOpen(); }}
            tabIndex={isFlipped ? 0 : -1}
            type="button"
          >
            상품 자세히 보기 <ArrowRight size={13} />
          </button>
          <em>카드를 다시 누르면 표지가 보여요</em>
        </div>
      </div>
    </div>
  );
};

const NextProductCard = ({ product, onSelect }) => (
  <div className="v3-curation-pair">
    <span>Next in Queue</span>
    <button className="v3-next-card" disabled={!product} onClick={onSelect} type="button">
      {product ? (
        <>
          <span>다음 추천 상품</span>
          <ProductCover product={product} variant="queue" />
          <strong>{product.title}</strong>
          <small>{product.author}</small>
          <div><b>{product.price}</b><span>{product.dDay}</span></div>
          <i><b style={{ width: `${product.progress}%` }}>{product.progress}%</b></i>
          <p>클릭하여 중앙에서 보기</p>
        </>
      ) : (
        <p className="v3-next-empty">대기 중인 다음 상품이 없습니다.</p>
      )}
    </button>
  </div>
);

const ShelfProduct = ({ product }) => {
  const starCount = Math.max(1, Math.min(5, Math.round(product.progress / 20)));

  return (
    <Link className="v3-shelf-product" to={`/buyer/products/${product.id}`}>
      <ProductCover product={product} variant="shelf" />
      <div className="v3-shelf-product-info">
        <div className="v3-stars" aria-label={`인기도 ${starCount}점`}>
          {[1, 2, 3, 4, 5].map((star) => (
            <Star key={star} className={star <= starCount ? 'is-filled' : ''} size={13} />
          ))}
        </div>
        <h3>{product.title}</h3>
        <p>{product.author}</p>
        <div><strong>{product.price}</strong><span>공구 참여</span></div>
      </div>
    </Link>
  );
};

const ClosingCard = ({ product }) => {
  const remaining = Math.max(product.target - product.current, 0);

  return (
    <article className="v3-closing-card">
      <span className={product.diffDays <= 1 ? 'is-today' : ''}>{product.dDay} 마감</span>
      <Link className="v3-closing-visual" to={`/buyer/products/${product.id}`}>
        <ProductCover product={product} variant="card" />
      </Link>
      <div className="v3-closing-copy">
        <div className="v3-card-meta"><span>{product.category}</span><small>{product.current}명 참여중</small></div>
        <Link to={`/buyer/products/${product.id}`}><h3>{product.title}</h3></Link>
        <div className="v3-card-progress"><span style={{ width: `${product.progress}%` }} /></div>
        <div className="v3-card-goal"><strong>달성률 {product.progress}%</strong><span>목표 {product.target}명 ({remaining}명 남음)</span></div>
        <div className="v3-card-footer">
          <div><del>{product.originalPrice}</del><strong>{product.price}</strong></div>
          <Link to={`/buyer/products/${product.id}`}>탑승하기</Link>
        </div>
      </div>
    </article>
  );
};

const HomePageV3 = () => {
  const navigate = useNavigate();
  const [products, setProducts] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [activeType, setActiveType] = useState('BOOK');
  const [featuredIndex, setFeaturedIndex] = useState(0);
  const [featuredFlipped, setFeaturedFlipped] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);

  useEffect(() => {
    const fetchProducts = async () => {
      try {
        const response = await fetch('http://localhost:8080/api/products');
        if (!response.ok) throw new Error('Failed to fetch products');

        const data = await response.json();
        setProducts(
          data
            .filter((item) => item.status === 'OPEN' && new Date(item.deadline) >= new Date())
            .map(mapProduct),
        );
      } catch (error) {
        console.error('Error fetching products:', error);
        setLoadError(true);
      } finally {
        setIsLoading(false);
      }
    };

    fetchProducts();
  }, []);

  const popularProducts = useMemo(
    () => [...products].sort((a, b) => b.progress - a.progress || a.diffDays - b.diffDays),
    [products],
  );
  const featuredCandidates = useMemo(() => {
    const matched = popularProducts.filter((product) => product.rawType === activeType);
    return matched.length > 0 ? matched : popularProducts;
  }, [activeType, popularProducts]);
  const featured = featuredCandidates.length > 0
    ? featuredCandidates[featuredIndex % featuredCandidates.length]
    : null;
  const nextProduct = featuredCandidates.length > 1
    ? featuredCandidates[(featuredIndex + 1) % featuredCandidates.length]
    : null;
  const shelfProducts = popularProducts.filter((product) => product.id !== featured?.id).slice(0, 4);
  const urgentProducts = [...products]
    .filter((product) => product.diffDays >= 0 && product.diffDays <= 5)
    .sort((a, b) => a.diffDays - b.diffDays || b.progress - a.progress);
  const closingProducts = (urgentProducts.length > 0 ? urgentProducts : popularProducts).slice(0, 4);
  const handleSearch = () => {
    const query = searchTerm.trim();
    navigate(query ? `/buyer/products?q=${encodeURIComponent(query)}` : '/buyer/products');
  };

  const selectType = (type) => {
    setActiveType(type);
    setFeaturedIndex(0);
    setFeaturedFlipped(false);
  };

  const selectNextProduct = () => {
    if (!nextProduct) return;
    setFeaturedIndex((index) => index + 1);
    setFeaturedFlipped(false);
  };

  return (
    <div className="v3-page">
      <main className="v3-main">
        <section className="v3-bookcase">
          <IntegratedHeader />

          <div className="v3-upper-shelf">
            <div className="v3-editorial-copy">
              <h1>Books <br />Popularity</h1>
              <p>유한대학교 전공도서 공동구매 최신 라인업</p>
              <div className="v3-search-box">
                <Search size={19} />
                <input
                  aria-label="상품 검색"
                  onChange={(event) => setSearchTerm(event.target.value)}
                  onKeyDown={(event) => { if (event.key === 'Enter') handleSearch(); }}
                  placeholder="Titles, author, or topics"
                  type="search"
                  value={searchTerm}
                />
                <button aria-label="검색" onClick={handleSearch} type="button"><ArrowRight size={18} /></button>
              </div>
            </div>

            <div className="v3-feature-column">
              <div className="v3-type-tabs">
                <button className={activeType === 'BOOK' ? 'is-active' : ''} onClick={() => selectType('BOOK')} type="button">
                  <BookOpen size={14} />도서
                </button>
                <button className={activeType === 'ITEM' ? 'is-active' : ''} onClick={() => selectType('ITEM')} type="button">
                  <ShoppingBag size={14} />학과 상품
                </button>
              </div>
              {isLoading ? (
                <div className="v3-feature-empty">상품을 불러오는 중...</div>
              ) : (
                <FeaturedProduct
                  isFlipped={featuredFlipped}
                  onFlip={() => setFeaturedFlipped((flipped) => !flipped)}
                  onOpen={() => navigate(`/buyer/products/${featured.id}`)}
                  product={featured}
                />
              )}
            </div>

            <div className="v3-curation-column">
              <NextProductCard product={nextProduct} onSelect={selectNextProduct} />
            </div>
          </div>

          <div className="v3-wood-shelf" aria-hidden="true"><i /><b /><span /></div>

          <div className="v3-lower-shelf vertical-label">
            <div className="v3-vertical-label"></div>
            <div className="v3-shelf-grid">
              {shelfProducts.map((product) => <ShelfProduct key={product.id} product={product} />)}
              {!isLoading && shelfProducts.length === 0 && (
                <div className="v3-empty-state">{loadError ? '상품 정보를 불러오지 못했습니다.' : '현재 모집 중인 상품이 없습니다.'}</div>
              )}
            </div>
          </div>
        </section>

        <section className="v3-closing-section">
          <div className="v3-closing-heading">
            <div>
              <p><span><Timer size={14} />마감 임박</span></p>
              <h2>마감 임박 전공 도서!</h2>
            </div>
            <Link to="/buyer/products">마감 임박 전체보기 <ArrowRight size={16} /></Link>
          </div>

          <div className="v3-closing-grid">
            {closingProducts.map((product) => <ClosingCard key={product.id} product={product} />)}
          </div>

          {!isLoading && closingProducts.length === 0 && (
            <div className="v3-closing-empty">
              <Users size={28} />
              <p>{loadError ? '상품 정보를 불러오지 못했습니다.' : '현재 모집 중인 공동구매가 없습니다.'}</p>
              <Link to="/buyer/products">전체 상품 보기</Link>
            </div>
          )}
        </section>
      </main>
    </div>
  );
};

export default HomePageV3;
