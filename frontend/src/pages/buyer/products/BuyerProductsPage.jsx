import React, { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import {
  BookOpen,
  CheckCircle,
  ChevronDown,
  Clock,
  Image as ImageIcon,
  LayoutGrid,
  List,
  Search,
  SlidersHorizontal,
  Users,
} from 'lucide-react';
import { getDisplayProductImageUrl } from '../../../utils/productImageUrl';
import {
  PRODUCT_CATEGORIES,
  getProductCategoryName,
  normalizeProductCategory,
} from '../../../constants/productCategories';
import { IntegratedHeader } from '../../home/homePage_v3';
import './BuyerProductsPage.css';

const PAGE_SIZE = 10;

const BuyerProductsPage = () => {
  const [searchParams] = useSearchParams();
  const [isAdvancedOpen, setIsAdvancedOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState(searchParams.get('q') || '');
  const [searchTarget, setSearchTarget] = useState('ALL');
  const [typeFilter, setTypeFilter] = useState('ALL');
  const [categoryFilter, setCategoryFilter] = useState('ALL');
  const [viewMode, setViewMode] = useState('GRID');
  const [sortFilter, setSortFilter] = useState('LATEST');
  const [showClosed, setShowClosed] = useState(false);
  const [productList, setProductList] = useState([]);
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [requestKey, setRequestKey] = useState(0);

  React.useEffect(() => {
    setSearchQuery(searchParams.get('q') || '');
  }, [searchParams]);

  React.useEffect(() => {
    let isCancelled = false;

    setIsLoading(true);
    setLoadError('');

    fetch('http://localhost:8080/api/products')
      .then((res) => {
        if (!res.ok) throw new Error('상품 목록을 불러오지 못했습니다.');
        return res.json();
      })
      .then((data) => {
        if (isCancelled) return;

        const formattedData = data.map((item) => ({
          id: item.productId,
          title: item.title,
          type: item.type,
          category: normalizeProductCategory(item.category),
          major: item.type === 'BOOK' ? getProductCategoryName(item.category) : '학과 물품',
          author: item.author || item.publisher || '정보 없음',
          current: item.currentCount,
          target: item.targetCount,
          price: item.price.toLocaleString() + '원',
          status: item.status === 'OPEN' ? '모집 중' : '마감됨',
          deadline: item.deadline ? item.deadline.split('T')[0] : '기한 없음',
          thumbnail: getDisplayProductImageUrl(item.imageUrl),
          description: item.description || '',
        }));

        setProductList(formattedData);
        setIsLoading(false);
      })
      .catch((error) => {
        if (isCancelled) return;
        console.error('상품 목록 로드 실패:', error);
        setLoadError('상품을 불러오는 중 문제가 생겼습니다. 잠시 후 다시 시도해 주세요.');
        setIsLoading(false);
      });

    return () => {
      isCancelled = true;
    };
  }, [requestKey]);

  const filteredList = React.useMemo(() => {
    const filtered = productList.filter((item) => {
      if (!showClosed && item.status === '마감됨') return false;
      if (typeFilter !== 'ALL' && item.type !== typeFilter) return false;
      if (categoryFilter !== 'ALL' && item.category !== categoryFilter) return false;

      if (searchQuery.trim() !== '') {
        const query = searchQuery.toLowerCase();
        const matchesTitle = item.title.toLowerCase().includes(query)
          || item.author.toLowerCase().includes(query);
        const matchesContent = item.description.toLowerCase().includes(query);

        if (searchTarget === 'TITLE' && !matchesTitle) return false;
        if (searchTarget === 'CONTENT' && !matchesContent) return false;
        if (searchTarget === 'ALL' && !(matchesTitle || matchesContent)) return false;
      }

      return true;
    });

    return filtered.sort((a, b) => {
      if (sortFilter === 'LATEST') return b.id - a.id;
      if (sortFilter === 'DEADLINE') {
        if (a.deadline === '기한 없음') return 1;
        if (b.deadline === '기한 없음') return -1;
        return new Date(a.deadline) - new Date(b.deadline);
      }
      if (sortFilter === 'PRICE_LOW') {
        const priceA = parseInt(a.price.replace(/[^0-9]/g, ''), 10) || 0;
        const priceB = parseInt(b.price.replace(/[^0-9]/g, ''), 10) || 0;
        return priceA - priceB;
      }
      return 0;
    });
  }, [productList, searchQuery, searchTarget, typeFilter, categoryFilter, sortFilter, showClosed]);

  React.useEffect(() => {
    setVisibleCount(PAGE_SIZE);
  }, [searchQuery, searchTarget, typeFilter, categoryFilter, sortFilter, showClosed]);

  const visibleList = filteredList.slice(0, visibleCount);

  const handleTypeChange = (type) => {
    setTypeFilter(type);
    if (type !== 'BOOK') setCategoryFilter('ALL');
  };

  const resetFilters = () => {
    setSearchQuery('');
    setSearchTarget('ALL');
    setTypeFilter('ALL');
    setCategoryFilter('ALL');
    setShowClosed(false);
  };

  return (
    <div className="buyer-products-page v3-design-page">
      <div className="buyer-products-header-shell v3-design-header-shell">
        <div className="buyer-products-container v3-design-container">
          <IntegratedHeader />
        </div>
      </div>

      <section className="buyer-products-masthead">
        <div className="buyer-products-container v3-design-container">
          <div className="buyer-products-heading">
            <p>유한대학교 공동구매</p>
            <h1>함께 고르고, 더 좋은 가격으로</h1>
            <span>전공도서와 학과 물품을 한곳에서 찾아보세요.</span>
          </div>
        </div>
      </section>

      <main className="buyer-products-container v3-design-container buyer-products-main">
        <div className="buyer-mobile-filter-row">
          <button
            type="button"
            className={isAdvancedOpen ? 'is-active' : ''}
            onClick={() => setIsAdvancedOpen((isOpen) => !isOpen)}
            aria-expanded={isAdvancedOpen}
          >
            <SlidersHorizontal size={17} />
            상세 필터
          </button>
        </div>

        <div className="buyer-catalog-layout v3-design-panel">
          <aside className={`buyer-filter-panel ${isAdvancedOpen ? 'is-open' : ''}`}>
            <div className="buyer-filter-heading">
              <h2>상세 필터</h2>
              <button type="button" onClick={resetFilters}>초기화</button>
            </div>

            <div className="buyer-filter-search">
              <label htmlFor="buyer-product-search">상품 검색</label>
              <div>
                <Search size={16} aria-hidden="true" />
                <input
                  id="buyer-product-search"
                  type="search"
                  placeholder="도서명, 저자, 상품명"
                  value={searchQuery}
                  onChange={(event) => setSearchQuery(event.target.value)}
                />
              </div>
            </div>
            <br />
            <fieldset className="buyer-filter-group">
              <legend><Search size={15} />검색 범위</legend>
              {[
                ['ALL', '제목 + 내용'],
                ['TITLE', '제목만'],
                ['CONTENT', '내용만'],
              ].map(([target, label]) => (
                <label key={target}>
                  <input
                    type="radio"
                    name="searchTarget"
                    value={target}
                    checked={searchTarget === target}
                    onChange={() => setSearchTarget(target)}
                  />
                  <span>{label}</span>
                </label>
              ))}
            </fieldset>
            <br />
            <fieldset className="buyer-filter-group">
              <legend><BookOpen size={15} />상품 종류</legend>
              {[
                ['ALL', '전체 상품'],
                ['BOOK', '전공도서'],
                ['ITEM', '학과물품'],
              ].map(([type, label]) => (
                <label key={type}>
                  <input
                    type="radio"
                    name="productType"
                    value={type}
                    checked={typeFilter === type}
                    onChange={() => handleTypeChange(type)}
                  />
                  <span>{label}</span>
                </label>
              ))}
            </fieldset>

            <div className="buyer-filter-group">
              <label className="buyer-filter-label" htmlFor="department-filter">학과 분류</label>
              <div className="buyer-select-wrap">
                <select
                  id="department-filter"
                  value={categoryFilter}
                  onChange={(event) => {
                    const category = event.target.value;
                    setCategoryFilter(category);
                    if (category !== 'ALL') setTypeFilter('BOOK');
                  }}
                >
                  <option value="ALL">전체 학과</option>
                  {PRODUCT_CATEGORIES.map(({ code, name }) => (
                    <option key={code} value={code}>{name}</option>
                  ))}
                </select>
                <ChevronDown size={16} aria-hidden="true" />
              </div>
            </div>

            <label className="buyer-closed-toggle">
              <span><CheckCircle size={15} />마감된 상품 포함</span>
              <input
                type="checkbox"
                checked={showClosed}
                onChange={(event) => setShowClosed(event.target.checked)}
              />
            </label>
          </aside>

          <section className="buyer-product-results" aria-live="polite">
            <div className="buyer-results-toolbar">
              <div>
                <h2>상품 목록</h2>
                <span>총 <strong>{filteredList.length}</strong>개의 공구</span>
              </div>

              <div className="buyer-toolbar-actions">
                <div className="buyer-view-switch" aria-label="보기 방식">
                  <button
                    type="button"
                    className={viewMode === 'GRID' ? 'is-active' : ''}
                    onClick={() => setViewMode('GRID')}
                    title="그리드 보기"
                    aria-pressed={viewMode === 'GRID'}
                  >
                    <LayoutGrid size={17} />
                  </button>
                  <button
                    type="button"
                    className={viewMode === 'LIST' ? 'is-active' : ''}
                    onClick={() => setViewMode('LIST')}
                    title="목록 보기"
                    aria-pressed={viewMode === 'LIST'}
                  >
                    <List size={17} />
                  </button>
                </div>

                <label className="buyer-sort-select">
                  <span className="sr-only">정렬 방식</span>
                  <select value={sortFilter} onChange={(event) => setSortFilter(event.target.value)}>
                    <option value="LATEST">최신 등록순</option>
                    <option value="DEADLINE">마감 임박순</option>
                    <option value="PRICE_LOW">낮은 가격순</option>
                  </select>
                  <ChevronDown size={15} aria-hidden="true" />
                </label>
              </div>
            </div>

            {isLoading && (
              <div className={`buyer-product-list is-${viewMode.toLowerCase()}`} aria-label="상품을 불러오는 중">
                {Array.from({ length: viewMode === 'GRID' ? 8 : 4 }).map((_, index) => (
                  <div className="buyer-product-skeleton" key={index} aria-hidden="true">
                    <div />
                    <span />
                    <span />
                    <span />
                  </div>
                ))}
              </div>
            )}

            {!isLoading && loadError && (
              <div className="buyer-products-state" role="alert">
                <BookOpen size={32} />
                <h3>상품 목록을 열지 못했어요</h3>
                <p>{loadError}</p>
                <button type="button" onClick={() => setRequestKey((key) => key + 1)}>다시 시도</button>
              </div>
            )}

            {!isLoading && !loadError && filteredList.length === 0 && (
              <div className="buyer-products-state">
                <Search size={32} />
                <h3>조건에 맞는 상품이 없습니다</h3>
                <p>검색어나 필터를 바꾸면 더 많은 상품을 볼 수 있습니다.</p>
                <button type="button" onClick={resetFilters}>필터 초기화</button>
              </div>
            )}

            {!isLoading && !loadError && visibleList.length > 0 && (
              <div className={`buyer-product-list is-${viewMode.toLowerCase()}`}>
                {visibleList.map((item) => (
                  <Link
                    key={item.id}
                    to={`/buyer/products/${item.id}`}
                    className="buyer-product-card"
                  >
                    <div className="buyer-product-image">
                      <div className="buyer-image-fallback">
                        <ImageIcon size={30} />
                        <span>이미지 없음</span>
                      </div>
                      {item.thumbnail && (
                        <img
                          src={item.thumbnail}
                          alt={item.title}
                          onError={(event) => { event.currentTarget.style.display = 'none'; }}
                          className={item.type === 'BOOK' ? 'is-book' : ''}
                        />
                      )}
                    </div>

                    <div className="buyer-product-info">
                      <div className="buyer-product-meta">
                        <span>{item.major}</span>
                        <strong className={item.status === '모집 중' ? 'is-open' : 'is-closed'}>
                          {item.status}
                        </strong>
                      </div>

                      <div className="buyer-product-copy">
                        <h3>{item.title}</h3>
                        <p>{item.author}</p>
                      </div>

                      <div className="buyer-product-purchase">
                        <strong>{item.price}</strong>
                        <div>
                          <span><Clock size={13} />{item.deadline}</span>
                          <span><Users size={13} />{item.current} / {item.target}명</span>
                        </div>
                      </div>
                    </div>
                  </Link>
                ))}
              </div>
            )}

            {!isLoading && !loadError && visibleCount < filteredList.length && (
              <div className="buyer-load-more">
                <button type="button" onClick={() => setVisibleCount((count) => count + PAGE_SIZE)}>
                  상품 더보기
                  <span>{visibleCount} / {filteredList.length}</span>
                </button>
              </div>
            )}
          </section>
        </div>
      </main>
    </div>
  );
};

export default BuyerProductsPage;
