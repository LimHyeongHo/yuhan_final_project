import React, { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Store, PlusCircle, BookOpen, Package, DollarSign, Users, FileText, Upload, AlertCircle, X, Search } from 'lucide-react';
import Header from '../../../components/layout/Header';

const ProductRegisterPage = () => {
  const navigate = useNavigate();
  const submitLockRef = useRef(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 1. 등록 유형 상태 관리 ('BOOK' 또는 'ITEM')
  const [productType, setProductType] = useState('BOOK');

  // [신규] 바코드 관련 상태 및 감지 로직
  const [barcode, setBarcode] = useState('');
  const [isSearching, setIsSearching] = useState(false);
  const [searchResults, setSearchResults] = useState([]);
  const [isModalOpen, setIsModalOpen] = useState(false);

  const handleSelectProduct = (data) => {
    setFormData(prev => ({
      ...prev,
      title: data.title || '',
      publisher: data.brand || data.maker || data.mallName || '',
      author: data.author || '',
      imageUrl: data.image || '',
      price: data.price || '',
      description: data.description || '',
      category: data.category || '',
      isbn: data.isbn || '', // [신규] 바코드 검색 결과에서 ISBN 저장
      originalPrice: data.price || '', // [신규] 원가(정가) 저장
    }));
    if (data.image) {
      setImagePreview(data.image); // 검색된 이미지를 미리보기 화면에 띄움
    } else {
      setImagePreview(null);
    }
    setIsModalOpen(false);
    setSearchResults([]);
  };

  const handleBarcodeKeyDown = async (e) => {
    if (e.key === 'Enter') {
      e.preventDefault(); 
      if (!barcode.trim()) return;
      
      setIsSearching(true);
      try {
        const response = await fetch(`http://localhost:8080/api/search/product?query=${barcode}&type=${productType}`);
        if (response.ok) {
          const dataList = await response.json();
          if (dataList.length === 1) {
            handleSelectProduct(dataList[0]);
            alert("검색 완료! 항목이 새로 채워졌습니다.");
          } else if (dataList.length > 1) {
            setSearchResults(dataList);
            setIsModalOpen(true);
          } else {
            alert("검색 결과가 없습니다.");
          }
        } else {
          // 검색 실패 시 이전 데이터 싹 지우기
          setFormData(prev => ({
            ...prev,
            title: '',
            publisher: '',
            author: '',
            imageUrl: '',
          }));
          setImagePreview(null);
          
          try {
            const errorData = await response.json();
            if (errorData.length > 0 && errorData[0].error) {
              alert(errorData[0].error);
            } else {
              alert("상품을 찾을 수 없습니다. 직접 입력해 주세요.");
            }
          } catch(e) {
            alert("상품을 찾을 수 없습니다. 직접 입력해 주세요.");
          }
        }
      } catch(error) {
        alert("검색 중 오류가 발생했습니다. 백엔드 서버를 확인해 주세요.");
      } finally {
        setIsSearching(false);
        setBarcode(''); 
      }
    }
  };

  // 2. 입력 데이터 상태 관리
  const [formData, setFormData] = useState({
    title: '',        // 도서명 또는 물품명
    author: '',       // 저자 (도서 전용)
    publisher: '',    // 출판사 또는 제조사/브랜드
    price: '',
    targetCount: '',
    description: '',
    imageUrl: '',     // [신규] 네이버 등에서 가져온 외부 이미지 URL 저장용
    category: '',     // [신규] API에서 추출된 카테고리 정보
    isbn: '',         // [신규] 알라딘 및 블록체인 검증용 ISBN
    originalPrice: '', // [신규] 정가
  });
  // 2-1. 이미지 업로드용 함수
  const [imageFile, setImageFile] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);

  const handleImageChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      setImageFile(file);
      setImagePreview(URL.createObjectURL(file));
    }
  };

  const handleRemoveImage = () => {
    setImageFile(null);
    setImagePreview(null);
  };

  const handleChange = (e) => {
    const { id, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [id]: value
    }));
  };

  // 3. 유형 변경 시 입력 폼 초기화 및 상태값 변경 함수
  const handleTypeChange = (type) => {
    if (type === 'ITEM') {
      window.alert('네이버 상품 검색 API 문제를 해결하는 동안 학과 물품을 등록할 수 없습니다.');
      return;
    }

    setProductType(type);
    setFormData({
      title: '',
      author: '',
      publisher: '',
      price: '',
      targetCount: '',
      description: '',
      imageUrl: '',
      category: '',
      isbn: '',
      originalPrice: '',
    });
    setImagePreview(null); // 유형 변경 시 이미지 미리보기도 초기화
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (submitLockRef.current) return;

    if (Number(formData.price) <= 0 || Number(formData.targetCount) <= 0) {
      alert("가격과 목표 인원은 0보다 큰 수치여야 합니다.");
      return;
    }

    submitLockRef.current = true;
    setIsSubmitting(true);

    // 🌟 이미지가 포함된 데이터를 보낼 때는 FormData를 사용합니다
    const submitData = new FormData();
    submitData.append('type', productType);
    submitData.append('title', formData.title);
    submitData.append('price', formData.price);
    submitData.append('targetCount', formData.targetCount);
    submitData.append('description', formData.description);
    if (formData.imageUrl) submitData.append('imageUrl', formData.imageUrl); // URL 이미지 추가
    if (formData.category) submitData.append('category', formData.category); // 카테고리 추가
    if (formData.isbn) submitData.append('isbn', formData.isbn); // ISBN 추가
    if (formData.originalPrice) submitData.append('originalPrice', formData.originalPrice); // 정가 추가

    if (productType === 'BOOK') {
      submitData.append('author', formData.author);
    }
    submitData.append('publisher', formData.publisher);

    if (imageFile) {
      submitData.append('image', imageFile); // 파일 객체 담기
    }

    try {
      const response = await fetch('http://localhost:8080/api/products', {
        method: 'POST',
        credentials: 'include', // [수정] 로그인 세션 쿠키 포함
        headers: {
          'Idempotency-Key': crypto.randomUUID(),
        },
        body: submitData, // FormData 전송 시 Content-Type은 브라우저가 자동으로 multipart/form-data로 설정함
      });

      if (!response.ok) {
        throw new Error('상품 등록 실패');
      }

      alert(`${productType === 'BOOK' ? '도서' : '학과 물품'} 공동구매 등록이 완료되었습니다!`);
      navigate('/seller/dashboard'); // 성공 시 이동
    } catch (error) {
      console.error('등록 에러:', error);
      alert('상품 등록 중 오류가 발생했습니다.');
    } finally {
      submitLockRef.current = false;
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col text-gray-900 ">
      <Header />

      {/* 상단 다크 배너 */}
      <section className="bg-slate-900 text-white py-12 px-6 shadow-md">
        <div className="max-w-7xl mx-auto flex flex-col gap-2">
          <span className="bg-emerald-500/20 text-emerald-400 text-xs font-bold uppercase tracking-widest px-3 py-1 rounded-full w-max border border-emerald-500/30 flex items-center gap-1.5">
            <Store size={14} /> Seller Hub
          </span>
          <h2 className="text-4xl font-extrabold tracking-tight mt-1">
            물품 등록
          </h2>
          <p className="text-slate-400 font-medium text-base max-w-2xl mt-1">
            전공 도서 및 학과 생활에 필요한 실습 물품을 등록하고 공동구매 프로젝트를 개설하세요.
          </p>
        </div>
      </section>

      <main className="flex-grow max-w-4xl w-full mx-auto p-6 md:p-8 flex flex-col gap-8">

        {/* 등록 유형 선택 토글 탭 */}
        <div className="flex gap-2 p-1.5 bg-gray-200/70 rounded-2xl w-full sm:w-max mx-auto shadow-inner">
          <button
            onClick={() => handleTypeChange('BOOK')}
            className={`flex-1 sm:flex-none px-8 py-3 rounded-xl font-bold text-sm flex items-center justify-center gap-2 transition-all duration-300 ${productType === 'BOOK'
              ? 'bg-white text-blue-600 shadow-sm ring-1 ring-gray-900/5'
              : 'text-gray-500 hover:text-gray-700 hover:bg-gray-200/50'
              }`}
          >
            <BookOpen size={18} /> 전공 도서
          </button>
          <button
            onClick={() => handleTypeChange('ITEM')}
            className={`flex-1 sm:flex-none px-8 py-3 rounded-xl font-bold text-sm flex items-center justify-center gap-2 transition-all duration-300 ${productType === 'ITEM'
              ? 'bg-white text-blue-600 shadow-sm ring-1 ring-gray-900/5'
              : 'text-gray-500 hover:text-gray-700 hover:bg-gray-200/50'
              }`}
          >
            <Package size={18} /> 학과 물품
          </button>
        </div>

        {/* 입력 폼 전체 카드 패널 */}
        <form onSubmit={handleSubmit} className="bg-white rounded-[28px] p-8 md:p-10 border border-gray-200 shadow-sm flex flex-col gap-8">

          {/* 섹션 1: 기본 정보 */}
          <div className="flex flex-col gap-5">
            <h3 className="text-xl font-extrabold text-gray-950 tracking-tight flex items-center gap-2 border-b border-gray-100 pb-3">
              {productType === 'BOOK' ? <BookOpen size={20} className="text-blue-600" /> : <Package size={20} className="text-blue-600" />}
              {productType === 'BOOK' ? '도서 기본 정보' : '물품 기본 정보'}
            </h3>

            {/* [신규] 바코드 스캔 영역 */}
            <div className="flex flex-col gap-1.5 mb-2 bg-blue-50 p-4 rounded-xl border border-blue-100">
              <label htmlFor="barcode" className="text-sm font-bold text-blue-800 flex items-center gap-2">
                <Search size={16} /> {productType === 'BOOK' ? '바코드 스캔 (또는 직접 입력 후 검색)' : '상품 이름 검색'}
              </label>
              <div className="flex gap-2">
                <input
                  type="text" id="barcode"
                  value={barcode} 
                  onChange={(e) => setBarcode(e.target.value)}
                  onKeyDown={handleBarcodeKeyDown}
                  placeholder={productType === 'BOOK' ? "스캐너로 찍거나, 직접 상품명 입력 후 우측 검색 버튼 클릭" : "직접 상품명 입력 후 우측 검색 버튼 클릭"}
                  className="flex-grow p-3.5 rounded-xl border border-blue-200 bg-white focus:ring-2 focus:ring-blue-500 outline-none transition text-base font-bold text-gray-900"
                  disabled={isSearching}
                />
                <button
                  type="button"
                  onClick={() => handleBarcodeKeyDown({ key: 'Enter', preventDefault: () => {} })}
                  disabled={isSearching}
                  className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-3.5 rounded-xl font-bold transition whitespace-nowrap"
                >
                  {isSearching ? '검색 중...' : '검색'}
                </button>
              </div>
            </div>

            {/* 제목/물품명 */}
            <div className="flex flex-col gap-1.5">
              <label htmlFor="title" className="text-sm font-bold text-gray-700">
                {productType === 'BOOK' ? '전공서적 명' : '물품 명'}
              </label>
              <input
                type="text" id="title" required
                value={formData.title} onChange={handleChange}
                placeholder={productType === 'BOOK' ? "예) 컴퓨터 구조 및 설계 6판" : "예) 카시오 공학용 계산기 fx-991EX"}
                className="w-full p-3.5 rounded-xl border border-gray-200 bg-gray-50 focus:ring-2 focus:ring-blue-500 focus:bg-white outline-none transition text-base font-medium"
              />
            </div>

            {/* 분류(카테고리) - 신규 추가 */}
            <div className="flex flex-col gap-1.5">
              <label htmlFor="category" className="text-sm font-bold text-gray-700">분류 (카테고리)</label>
              <input
                type="text" id="category"
                value={formData.category} onChange={handleChange}
                placeholder="예) 컴퓨터/IT (검색 시 자동 입력됨)"
                className="w-full p-3.5 rounded-xl border border-gray-200 bg-gray-50 focus:ring-2 focus:ring-blue-500 focus:bg-white outline-none transition text-base font-medium"
              />
            </div>

            {/* 조건부 렌더링: 저자 및 출판사/제조사 */}
            <div className={`grid grid-cols-1 ${productType === 'BOOK' ? 'md:grid-cols-2' : ''} gap-5`}>
              {productType === 'BOOK' && (
                <div className="flex flex-col gap-1.5">
                  <label htmlFor="author" className="text-sm font-bold text-gray-700">저자</label>
                  <input
                    type="text" id="author" required
                    value={formData.author} onChange={handleChange}
                    placeholder="예) David A. Patterson"
                    className="w-full p-3.5 rounded-xl border border-gray-200 bg-gray-50 focus:ring-2 focus:ring-blue-500 focus:bg-white outline-none transition text-base font-medium"
                  />
                </div>
              )}
              <div className="flex flex-col gap-1.5">
                <label htmlFor="publisher" className="text-sm font-bold text-gray-700">
                  {productType === 'BOOK' ? '출판사' : '제조사 / 브랜드'}
                </label>
                <input
                  type="text" id="publisher" required
                  value={formData.publisher} onChange={handleChange}
                  placeholder={productType === 'BOOK' ? "예) 한티미디어" : "예) 카시오 (CASIO)"}
                  className="w-full p-3.5 rounded-xl border border-gray-200 bg-gray-50 focus:ring-2 focus:ring-blue-500 focus:bg-white outline-none transition text-base font-medium"
                />
              </div>
            </div>
          </div>

          {/* 섹션 2: 가격 및 조건 */}
          <div className="flex flex-col gap-5">
            <h3 className="text-xl font-extrabold text-gray-950 tracking-tight flex items-center gap-2 border-b border-gray-100 pb-3">
              <DollarSign size={20} className="text-blue-600" /> 공동구매 가격 및 조건
            </h3>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
              <div className="flex flex-col gap-1.5">
                <label htmlFor="price" className="text-sm font-bold text-gray-700">공동구매 제안 가격 (₩)</label>
                <div className="relative">
                  <span className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 font-bold text-sm">₩</span>
                  <input
                    type="number" id="price" required
                    value={formData.price} onChange={handleChange}
                    placeholder="예) 35000"
                    className="w-full p-3.5 pl-9 rounded-xl border border-gray-200 bg-gray-50 focus:ring-2 focus:ring-blue-500 focus:bg-white outline-none transition text-base font-semibold"
                  />
                </div>
              </div>
              <div className="flex flex-col gap-1.5">
                <label htmlFor="targetCount" className="text-sm font-bold text-gray-700">목표 달성 인원 (명)</label>
                <div className="relative">
                  <Users className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
                  <input
                    type="number" id="targetCount" required
                    value={formData.targetCount} onChange={handleChange}
                    placeholder="예) 10"
                    className="w-full p-3.5 pl-11 rounded-xl border border-gray-200 bg-gray-50 focus:ring-2 focus:ring-blue-500 focus:bg-white outline-none transition text-base font-semibold"
                  />
                </div>
              </div>
            </div>
          </div>

          {/* 섹션 3: 이미지 및 상세 설명 */}
          <div className="flex flex-col gap-5">
            <h3 className="text-xl font-extrabold text-gray-950 tracking-tight flex items-center gap-2 border-b border-gray-100 pb-3">
              <FileText size={20} className="text-blue-600" /> 상세 설명 및 실물 인증
            </h3>

            <div className="flex flex-col gap-1.5">
              <label className="text-sm font-bold text-gray-700">
                {productType === 'BOOK' ? '도서 실물 이미지 등록' : '물품 실물 이미지 등록'} (최대 1장)
              </label>
              {/* 🌟 수정된 부분: 미리보기가 있으면 이미지를, 없으면 업로드 박스를 보여줍니다 */}
              {imagePreview ? (
                <div className="relative w-full sm:w-1/2 md:w-1/3 aspect-[4/3] rounded-2xl overflow-hidden border border-gray-200 group">
                  <img src={imagePreview} alt="미리보기" className="w-full h-full object-cover" />
                  <button
                    type="button"
                    onClick={handleRemoveImage}
                    className="absolute top-2 right-2 bg-white/90 text-red-500 p-1.5 rounded-full shadow-md hover:bg-red-50 hover:text-red-600 transition"
                  >
                    <X size={16} strokeWidth={3} />
                  </button>
                </div>
              ) : (
                <label className="border-2 border-dashed border-gray-200 rounded-2xl p-6 bg-gray-50 flex flex-col items-center justify-center gap-2 cursor-pointer hover:bg-gray-100/70 transition group">
                  <div className="w-10 h-10 rounded-full bg-white shadow-sm flex items-center justify-center text-gray-400 group-hover:text-blue-500 transition">
                    <Upload size={18} />
                  </div>
                  <span className="text-xs font-bold text-gray-700 mt-1 group-hover:text-blue-600 transition">
                    {productType === 'BOOK' ? '클릭하여 전공책 앞표지 업로드' : '클릭하여 물품의 전체 형태가 보이는 사진 업로드'}
                  </span>
                  <span className="text-[10px] text-gray-400">PNG, JPG 파일 지원 (최대 5MB)</span>

                  {/* 클릭 이벤트를 받아줄 숨겨진 input 태그 */}
                  <input
                    type="file"
                    accept="image/*"
                    onChange={handleImageChange}
                    className="hidden"
                  />
                </label>
              )}
            </div>

            <div className="flex flex-col gap-1.5">
              <label htmlFor="description" className="text-sm font-bold text-gray-700">상세 설명</label>
              <textarea
                id="description" required rows={5}
                value={formData.description} onChange={handleChange}
                placeholder={productType === 'BOOK' ? "학과 내 수업 연계 정보 등을 상세히 기재해 주세요." : "물품의 실측 사이즈 등을 상세히 기재해 주세요."}
                className="w-full p-4 rounded-xl border border-gray-200 bg-gray-50 focus:ring-2 focus:ring-blue-500 focus:bg-white outline-none transition text-base font-medium resize-none leading-relaxed"
              ></textarea>
            </div>
          </div>

          <div className="bg-amber-50 rounded-2xl p-4 border border-amber-100 flex items-start gap-3 mt-2">
            <AlertCircle size={18} className="text-amber-600 flex-shrink-0 mt-0.5" />
            <div className="flex flex-col gap-0.5">
              <span className="text-xs font-bold text-amber-800">허위 매물 및 변조 등록 금지 수칙</span>
              <p className="text-[11px] text-amber-700 leading-relaxed font-medium mt-0.5">
                등록된 데이터는 플랫폼 관리 센터에 의해 실시간 추적 검증됩니다. 의도적인 위변조나 허위 기재 적발 시 판매자 권한이 영구 제한될 수 있습니다.
              </p>
            </div>
          </div>

          <div className="flex gap-4 border-t border-gray-100 pt-6 mt-2">
            <button
              type="button" onClick={() => navigate('/seller/dashboard')}
              className="px-6 py-4 border border-gray-200 text-gray-600 text-base font-bold rounded-xl hover:bg-gray-100 transition"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="flex-grow py-4 bg-emerald-600 text-white rounded-xl font-bold text-lg shadow-lg shadow-emerald-100 hover:bg-emerald-700 transition flex items-center justify-center gap-2 disabled:bg-gray-400 disabled:cursor-not-allowed"
            >
              <PlusCircle size={20} /> {isSubmitting ? '등록 중...' : '공동구매 프로젝트 개설 완료'}
            </button>
          </div>
        </form>
      </main>

      {/* 검색 결과 리스트 팝업 모달 */}
      {isModalOpen && (
        <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-[24px] shadow-2xl w-full max-w-2xl max-h-[85vh] flex flex-col overflow-hidden animate-in fade-in zoom-in duration-200">
            <div className="p-6 border-b border-gray-100 flex justify-between items-center bg-gray-50/80">
              <h3 className="text-xl font-extrabold text-gray-950 flex items-center gap-2">
                <Search size={22} className="text-blue-600" />
                검색 결과 선택
              </h3>
              <button 
                onClick={() => { setIsModalOpen(false); setSearchResults([]); }}
                className="p-2.5 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-full transition-colors"
                type="button"
              >
                <X size={20} />
              </button>
            </div>
            
            <div className="overflow-y-auto p-4 flex flex-col gap-3 custom-scrollbar">
              {searchResults.map((item, idx) => (
                <div 
                  key={idx}
                  onClick={() => handleSelectProduct(item)}
                  className="flex gap-5 p-4 border border-gray-100 rounded-2xl hover:border-blue-400 hover:shadow-md hover:bg-blue-50/40 cursor-pointer transition-all group"
                >
                  {item.image ? (
                    <img src={item.image} alt={item.title} className="w-[72px] h-[96px] object-contain rounded-lg border border-gray-200 bg-white" />
                  ) : (
                    <div className="w-[72px] h-[96px] bg-gray-50 flex items-center justify-center rounded-lg border border-gray-200 text-gray-400 text-xs font-medium">
                      No Img
                    </div>
                  )}
                  <div className="flex flex-col flex-1 justify-center gap-1.5">
                    <h4 className="text-[15px] font-extrabold text-gray-900 group-hover:text-blue-700 leading-snug">{item.title}</h4>
                    <p className="text-sm font-medium text-gray-500">
                      {productType === 'BOOK' ? item.author : (item.brand || item.maker || item.mallName)}
                    </p>
                    <p className="text-sm font-black text-gray-900 mt-0.5">
                      {Number(item.price).toLocaleString()}원
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

    </div>
  );
};

export default ProductRegisterPage;
