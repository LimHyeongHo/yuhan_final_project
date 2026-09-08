// [수정][feature/ui-fixes] 더미 목록 대신 실제 등록 상품(GET /api/products/seller/me) 조회 + 카드 클릭 시 해당 게시글로 이동
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Store, Users, ChevronRight } from 'lucide-react';

const API_BASE = 'http://localhost:8080/api';

const STATUS_LABEL = {
  OPEN: { text: '모집 중', cls: 'text-blue-700 bg-blue-100' },
  CLOSED_SUCCESS: { text: '모집 성공', cls: 'text-emerald-700 bg-emerald-100' },
  CLOSED_FAIL: { text: '모집 실패', cls: 'text-red-700 bg-red-100' },
};

const formatDate = (value) =>
  value ? `${new Date(value).toLocaleDateString('ko-KR')} 등록` : '등록일 알 수 없음';

const MyPageProjects = () => {
  const navigate = useNavigate();
  const [projectList, setProjectList] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch(`${API_BASE}/products/seller/me`, { credentials: 'include' })
      .then((res) => {
        if (!res.ok) throw new Error('내 판매 프로젝트를 불러오지 못했습니다.');
        return res.json();
      })
      .then((data) => {
        setProjectList(data.map((item) => ({
          id: item.productId,
          title: item.title,
          date: formatDate(item.createdAt),
          status: STATUS_LABEL[item.status] || { text: item.status, cls: 'text-gray-600 bg-gray-100' },
          current: item.currentCount,
          target: item.targetCount,
        })));
      })
      .catch((err) => console.error('내 판매 프로젝트 로드 실패', err))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="bg-white rounded-[32px] p-8 border border-gray-200 shadow-sm flex flex-col gap-6">
      <div className="flex items-center gap-3 pb-6 border-b border-gray-100">
        <Store className="text-emerald-500" size={24} />
        <h4 className="text-lg font-bold text-gray-900">내 판매 프로젝트</h4>
      </div>

      <div className="flex flex-col gap-4">
        {loading ? (
          <div className="text-center text-gray-400 py-8 text-sm font-bold">불러오는 중...</div>
        ) : projectList.length === 0 ? (
          <div className="text-center text-gray-400 py-8 text-sm font-bold">개설한 공동구매가 없습니다.</div>
        ) : (
          projectList.map((project) => (
            <div
              key={project.id}
              onClick={() => navigate(`/buyer/products/${project.id}`, { state: { backButtonText: '개설한 공동구매 관리로 돌아가기' } })}
              className="flex items-center justify-between p-5 rounded-2xl border border-gray-100 hover:border-emerald-200 hover:shadow-sm transition cursor-pointer group"
            >
              <div className="flex flex-col gap-2">
                <div className="flex items-center gap-2">
                  <span className={`text-[11px] font-black px-2.5 py-1 rounded-md ${project.status.cls}`}>
                    {project.status.text}
                  </span>
                  <span className="text-xs text-gray-400 font-medium">{project.date}</span>
                </div>
                <h5 className="text-base font-extrabold text-gray-900 group-hover:text-emerald-600 transition">{project.title}</h5>

                <div className="flex items-center gap-1.5 text-sm font-bold text-gray-600 mt-1">
                  <Users size={14} className="text-gray-400" />
                  <span>모집 인원: <span className={project.current >= project.target ? 'text-emerald-600' : 'text-blue-600'}>{project.current}</span> / {project.target}명</span>
                </div>
              </div>
              <ChevronRight className="text-gray-300 group-hover:text-emerald-500 transition" size={20} />
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default MyPageProjects;
