import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Store, Edit2, BookOpen, Package, DollarSign, Users, FileText, Upload, AlertCircle, X, Search } from 'lucide-react';
import Header from '../../../components/layout/Header';

const ProductsEditPage = () => {
  const navigate = useNavigate();
  const { id } = useParams();

  // 1. 등록 유형 상태 관리 ('BOOK' 또는 'ITEM')
  const [productType, setProductType] = useState('BOOK');

  // [신규] 바코드 관련 상태 및 감지 로직
  const [barcode, setBarcode] = useState('');
  const [isSearching, setIsSearching] = useState(false);

  const handleBarcodeKeyDown = async (e) => {
    if (e.key === 'Enter') {
      e.preventDefault(); 
      if (!barcode.trim()) return;
      
      setIsSearching(true);
      try {
        const response = await fetch(`http://localhost:8080/api/search/product?query=${barcode}&type=${productType}`);
        if (response.ok) {
          const data = await response.json();
          setFormData(prev => ({
            ...prev,
            title: data.title || '',
            publisher: data.brand || data.maker || data.mallName || '',
            author: data.author || '',
            imageUrl: data.image || '',
            price: data.price || '',
            description: data.description || '',
          }));
          if (data.image) {
            setImagePreview(data.image); // 검색된 이미지를 미리보기 화면에 띄움
          } else {
            setImagePreview(null);
          }
          alert("바코드 검색 완료! 항목이 새로 채워졌습니다.");
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
            if (errorData.error) {
              alert(errorData.error);
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
    category: '',
    imageUrl: '',     // [신규] 네이버 등에서 가져온 외부 이미지 URL 저장용
  });
  // 2-1. 이미지 업로드용 함수
  const [imageFile, setImageFile] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);

  // 기존 데이터 불러오기
  useEffect(() => {
    fetch(`http://localhost:8080/api/products/${id}`)
      .then(res => res.json())
      .then(data => {
        setProductType(data.type || 'BOOK');
        setFormData({
          title: data.title || '',
          author: data.author || '',
          publisher: data.publisher || '',
          price: data.price || '',
          targetCount: data.targetCount || '',
          description: data.description || '',
          category: data.category || '',
          imageUrl: data.imageUrl || '',
        });
        if (data.imageUrl) {
          setImagePreview(data.imageUrl);
        }
      })
      .catch(err => {
        console.error("상품 정보 불러오기 실패:", err);
        alert("상품 정보를 불러오는데 실패했습니다.");
        navigate('/seller/status');
      });
  }, [id, navigate]);

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
    setProductType(type);
    setFormData({
      title: '',
      author: '',
      publisher: '',
      price: '',
      targetCount: '',
      description: '',
      imageUrl: '',
    });
    setImagePreview(null); // 유형 변경 시 이미지 미리보기도 초기화
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (Number(formData.price) <= 0 || Number(formData.targetCount) <= 0) {
      alert("가격과 목표 인원은 0보다 큰 수치여야 합니다.");
      return;
    }
    // [신규] PRD-RQ-005: 가격은 100원 단위로만 입력 가능
    if (Number(formData.price) % 100 !== 0) {
      alert("가격은 100원 단위로 입력해주세요.");
      return;
    }

    // 🌟 이미지가 포함된 데이터를 보낼 때는 FormData를 사용합니다
    const submitData = new FormData();
    submitData.append('type', productType);
    submitData.append('title', formData.title);
    submitData.append('price', formData.price);
    submitData.append('targetCount', formData.targetCount);
    submitData.append('description', formData.description);
    if (formData.category) submitData.append('category', formData.category);
    if (formData.imageUrl) submitData.append('imageUrl', formData.imageUrl); // URL 이미지 추가

    if (productType === 'BOOK') {
      submitData.append('author', formData.author);
    }
    submitData.append('publisher', formData.publisher);

    if (imageFile) {
      submitData.append('image', imageFile); // 파일 객체 담기
    }

    try {
      const response = await fetch(`http://localhost:8080/api/products/${id}`, {
        method: 'PUT',
        credentials: 'include', // [수정] 로그인 세션 쿠키 포함 (소유자 확인용)
        body: submitData, // FormData 전송 시 Content-Type은 브라우저가 자동으로 multipart/form-data로 설정함
      });

      if (response.ok) {
        alert("프로젝트가 성공적으로 수정되었습니다!");
        navigate('/seller/status');
      } else {
        const data = await response.json().catch(() => ({}));
        throw new Error(data.message || '상품 수정 실패');
      }
    } catch (error) {
      console.error('등록 에러:', error);
      alert(error.message || '상품 등록 중 오류가 발생했습니다.');
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col text-gray-900 ">
      <Header />

      {/* 상단 다크 배너 */}
      <section className="bg-slate-900 text-white py-12 px-6 shadow-md">
        <div className="max-w-4xl mx-auto flex flex-col gap-2">
          <span className="bg-blue-500/20 text-blue-400 text-xs font-bold uppercase tracking-widest px-3 py-1 rounded-full w-max border border-blue-500/30 flex items-center gap-1.5">
            <Store size={14} /> Seller Hub
          </span>
          <h2 className="text-4xl font-extrabold tracking-tight mt-1">프로젝트 수정하기</h2>
          <p className="text-slate-400 font-medium text-base mt-1">등록한 전공 도서나 학과 물품의 정보를 수정하세요.</p>
        </div>
      </section>

      <main className="flex-grow max-w-4xl w-full mx-auto p-6 md:p-8 flex flex-col gap-8">

        {/* 등록 유형 선택 토글 탭 */}
        <div className="flex gap-2 p-1.5 bg-gray-200/70 rounded-2xl w-full sm:w-max mx-auto shadow-inner opacity-80 pointer-events-none">
          <button
            type="button"
            className={`flex-1 sm:flex-none px-8 py-3 rounded-xl font-bold text-sm flex items-center justify-center gap-2 transition-all duration-300 ${productType === 'BOOK'
              ? 'bg-white text-blue-600 shadow-sm ring-1 ring-gray-900/5'
              : 'text-gray-500 hover:text-gray-700 hover:bg-gray-200/50'
              }`}
          >
            <BookOpen size={18} /> 전공 도서
          </button>
          <button
            type="button"
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
                  className="flex-grow p-3.5 rounded-xl border border-blue-200 bg-white focus:ring-2 focus:ring-blue-500 outline-none transition text-base font-bold text-gray-900 disabled:bg-gray-100 disabled:text-gray-400"
                  disabled={productType === 'BOOK' || isSearching}
                />
                <button
                  type="button"
                  onClick={() => handleBarcodeKeyDown({ key: 'Enter', preventDefault: () => {} })}
                  disabled={productType === 'BOOK' || isSearching}
                  className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-3.5 rounded-xl font-bold transition whitespace-nowrap disabled:bg-gray-400"
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
                disabled={productType === 'BOOK'}
                className="w-full p-3.5 rounded-xl border border-gray-200 bg-gray-50 focus:ring-2 focus:ring-blue-500 focus:bg-white outline-none transition text-base font-medium disabled:bg-gray-200 disabled:text-gray-500"
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
                    disabled={productType === 'BOOK'}
                    className="w-full p-3.5 rounded-xl border border-gray-200 bg-gray-50 focus:ring-2 focus:ring-blue-500 focus:bg-white outline-none transition text-base font-medium disabled:bg-gray-200 disabled:text-gray-500"
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
                  disabled={productType === 'BOOK'}
                  className="w-full p-3.5 rounded-xl border border-gray-200 bg-gray-50 focus:ring-2 focus:ring-blue-500 focus:bg-white outline-none transition text-base font-medium disabled:bg-gray-200 disabled:text-gray-500"
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
                    type="number" id="price" required step="100"
                    value={formData.price} onChange={handleChange}
                    onWheel={(e) => e.currentTarget.blur()}
                    placeholder="예) 35000"
                    disabled={productType === 'BOOK'}
                    className="w-full p-3.5 pl-9 rounded-xl border border-gray-200 bg-gray-50 focus:ring-2 focus:ring-blue-500 focus:bg-white outline-none transition text-base font-semibold disabled:bg-gray-200 disabled:text-gray-500"
                  />
                </div>
              </div>
              <div className="flex flex-col gap-1.5">
                <label htmlFor="targetCount" className="text-sm font-bold text-gray-700">목표 달성 인원 (명)</label>
                <div className="relative">
                  <Users className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
                  <input
                    type="number" id="targetCount" required step="1"
                    value={formData.targetCount} onChange={handleChange}
                    onWheel={(e) => e.currentTarget.blur()}
                    placeholder="예) 10"
                    disabled={productType === 'BOOK'}
                    className="w-full p-3.5 pl-11 rounded-xl border border-gray-200 bg-gray-50 focus:ring-2 focus:ring-blue-500 focus:bg-white outline-none transition text-base font-semibold disabled:bg-gray-200 disabled:text-gray-500"
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

          {/* 버튼 영역 */}
          <div className="pt-6 flex gap-4">
            <button 
              type="button" 
              onClick={() => navigate(-1)}
              className="flex-1 py-4 bg-gray-100 text-gray-700 font-bold rounded-2xl hover:bg-gray-200 transition text-lg"
            >
              취소
            </button>
            <button 
              type="submit" 
              className="flex-[2] py-4 bg-blue-600 text-white font-bold rounded-2xl hover:bg-blue-700 transition shadow-lg shadow-blue-200 text-lg flex items-center justify-center gap-2"
            >
              <Edit2 size={20} />
              수정 완료하기
            </button>
          </div>
        </form>
      </main>
    </div>
  );
};

export default ProductsEditPage;