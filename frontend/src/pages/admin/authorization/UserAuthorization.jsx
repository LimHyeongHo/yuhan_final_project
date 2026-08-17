import React, { useState, useEffect } from 'react';
import { Users, UserPlus, CheckCircle, Clock, ShieldAlert } from 'lucide-react';
import Header from '../../../components/layout/Header'; // 분리해둔 공통 헤더 불러오기


// 1. 판매자 승인 대기열 카드 컴포넌트
const ApprovalCard = ({ name, id, email, date, onGrant }) => (
  <div className="flex items-center justify-between p-5 border-b border-gray-100 last:border-b-0 hover:bg-gray-50/50 transition">
    <div className="flex items-center gap-4">
      <div className="w-12 h-12 bg-gray-200 rounded-full overflow-hidden border border-gray-300 flex-shrink-0">
        <img 
          src={`https://api.dicebear.com/7.x/notionists/svg?seed=${email}&backgroundColor=e2e8f0`} 
          alt="profile" 
          className="w-full h-full object-cover"
        />
      </div>
      <div className="flex flex-col">
        <div className="flex items-center gap-2">
          <span className="font-bold text-gray-900 text-base">{name}</span>
        </div>
        <span className="text-sm text-gray-500 mt-0.5">{email}</span>
      </div>
    </div>
    
    <div className="flex items-center gap-8">
      <div className="hidden md:flex flex-col text-right">
        <span className="text-xs font-bold text-gray-400">신청일자</span>
        <span className="text-sm font-medium text-gray-700">{date}</span>
      </div>
      <div className="flex gap-2">
        <button className="px-4 py-2 border border-gray-200 text-gray-600 rounded-xl text-sm font-bold hover:bg-gray-100 transition">
          심사
        </button>
        <button onClick={() => onGrant(email)} className="px-4 py-2 bg-blue-600 text-white rounded-xl text-sm font-bold shadow-md shadow-blue-200 hover:bg-blue-700 transition">
          권한 부여
        </button>
      </div>
    </div>
  </div>
);

const UserAuthorization = () => {
  const [pendingUsers, setPendingUsers] = useState([]);

  const fetchPendingUsers = () => {
    fetch('http://localhost:8080/api/admin/users/pending', { credentials: 'include' })
      .then(res => res.json())
      .then(data => setPendingUsers(data))
      .catch(err => console.error("대기열 로드 실패:", err));
  };

  useEffect(() => {
    fetchPendingUsers();
  }, []);

  const handleGrantRole = (email) => {
    fetch(`http://localhost:8080/api/admin/users/${email}/grant-seller`, { 
      method: 'POST', 
      credentials: 'include' 
    })
      .then(res => {
        if (!res.ok) throw new Error('권한 부여 실패');
        alert('판매자 권한이 성공적으로 부여되었습니다.');
        fetchPendingUsers(); // 목록 갱신
      })
      .catch(err => {
        console.error(err);
        alert(err.message);
      });
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col text-gray-900">
      
      {/* 1. 글로벌 헤더 (최상단 고정) */}
      <Header />

      {/* 2. 상단 묵직한 다크 보안 배너 (SecurityLogPage 스타일 계승) */}
      <section className="bg-slate-900 text-white py-12 px-6 shadow-md">
        <div className="max-w-7xl mx-auto flex flex-col gap-2">
          <span className="bg-blue-500/20 text-blue-400 text-xs font-bold uppercase tracking-widest px-3 py-1 rounded-full w-max border border-blue-500/30">
            Operations Hub
          </span>
          <h2 className="text-4xl font-extrabold tracking-tight">
            회원/권한 관리
          </h2>
          <p className="text-slate-400 font-medium text-base max-w-2xl mt-1">
            판매자 승인 대기열을 처리하고, 활성 사용자의 권한을 관리합니다. 플랫폼 내 트래픽 및 접속 상태를 실시간으로 모니터링할 수 있습니다.
          </p>
        </div>
      </section>

      {/* 3. 메인 콘텐츠 영역 */}
      <main className="flex-grow max-w-7xl w-full mx-auto p-6 md:p-8 flex flex-col gap-8">
        
        {/* 요약 통계 카드 3종 */}
        <section className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* 카드 1: 대기열 */}
          <div className="bg-white rounded-[24px] p-6 border border-gray-200 shadow-sm flex justify-between items-center">
            <div className="flex flex-col gap-1">
              <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">판매자 승인 대기</span>
              <h3 className="text-3xl font-black text-gray-950 mt-1">05 건</h3>
              <span className="text-blue-600 text-xs font-bold mt-1 flex items-center gap-1">
                <Clock size={14} /> ACTION REQUIRED
              </span>
            </div>
            <div className="w-12 h-12 rounded-full bg-blue-50 flex items-center justify-center text-blue-600">
              <UserPlus size={24} />
            </div>
          </div>

          {/* 카드 2: 신규 가입 */}
          <div className="bg-white rounded-[24px] p-6 border border-gray-200 shadow-sm flex justify-between items-center">
            <div className="flex flex-col gap-1">
              <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">일간 신규 가입</span>
              <h3 className="text-3xl font-black text-gray-950 mt-1">1,284 명</h3>
              <span className="text-emerald-600 text-xs font-bold mt-1">
                ▲ +12% VS YESTERDAY
              </span>
            </div>
            <div className="w-12 h-12 rounded-full bg-emerald-50 flex items-center justify-center text-emerald-600">
              <Users size={24} />
            </div>
          </div>

          {/* 카드 3: 누적 판매자 */}
          <div className="bg-white rounded-[24px] p-6 border border-gray-200 shadow-sm flex justify-between items-center">
            <div className="flex flex-col gap-1">
              <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">누적 활성 판매자</span>
              <h3 className="text-3xl font-black text-gray-950 mt-1">452 명</h3>
              <span className="text-emerald-600 text-xs font-bold mt-1">
                ▲ +3% VS LAST MONTH
              </span>
            </div>
            <div className="w-12 h-12 rounded-full bg-purple-50 flex items-center justify-center text-purple-600">
              <CheckCircle size={24} />
            </div>
          </div>
        </section>

        {/* 하단 리스트 및 모니터링 영역 */}
        <section className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          
          {/* 좌측: 판매자 승인 대기열 (2/3 영역 차지) */}
          <div className="lg:col-span-2 bg-white rounded-[28px] p-6 md:p-8 border border-gray-200 shadow-sm flex flex-col">
            <div className="flex justify-between items-center mb-6">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center">
                  <UserPlus size={18} />
                </div>
                <h3 className="text-xl font-extrabold text-gray-950 tracking-tight">판매자 승인 대기열</h3>
              </div>
              <span className="bg-blue-50 text-blue-600 text-xs font-bold px-3 py-1.5 rounded-lg">
                5건 대기 중
              </span>
            </div>
            
            <div className="flex flex-col border-t border-gray-100">
              {pendingUsers.length === 0 ? (
                <div className="p-8 text-center text-gray-400 font-bold">승인 대기 중인 판매자가 없습니다.</div>
              ) : (
                pendingUsers.map(user => (
                  <ApprovalCard 
                    key={user.email} 
                    name={user.nickname} 
                    email={user.email} 
                    date={user.createdAt} 
                    onGrant={handleGrantRole}
                  />
                ))
              )}
            </div>
            
            <button className="w-full mt-4 py-3 bg-gray-50 text-gray-600 text-sm font-bold rounded-xl hover:bg-gray-100 transition">
              대기열 전체 보기
            </button>
          </div>

          {/* 우측: 상세 활동 모니터링 (1/3 영역 차지) */}
          <div className="lg:col-span-1 bg-white rounded-[28px] p-6 md:p-8 border border-gray-200 shadow-sm flex flex-col">
            <div className="flex justify-between items-center mb-6">
              <h3 className="text-xl font-extrabold text-gray-950 tracking-tight">상세 활동 모니터링</h3>
              <span className="bg-emerald-50 text-emerald-600 text-[10px] font-extrabold px-2.5 py-1 rounded-full uppercase tracking-wider flex items-center gap-1.5">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span> Live Traffic
              </span>
            </div>

            <div className="flex flex-col gap-6 mt-2">
              {/* 접속 세션 */}
              <div className="flex flex-col gap-2">
                <span className="text-xs font-bold text-gray-500">현재 접속 세션</span>
                <div className="flex items-end gap-2">
                  <h4 className="text-2xl font-black text-gray-900">4,209</h4>
                  <span className="text-sm font-medium text-gray-400 mb-1">users</span>
                </div>
                <div className="w-full h-1.5 bg-gray-100 rounded-full overflow-hidden">
                  <div className="h-full bg-blue-600 w-[75%] rounded-full"></div>
                </div>
              </div>

              {/* 초당 요청 */}
              <div className="flex flex-col gap-2">
                <span className="text-xs font-bold text-gray-500">초당 API 요청 (RPS)</span>
                <div className="flex items-end gap-2">
                  <h4 className="text-2xl font-black text-gray-900">842</h4>
                  <span className="text-sm font-medium text-gray-400 mb-1">req/s</span>
                </div>
                <div className="w-full h-1.5 bg-gray-100 rounded-full overflow-hidden">
                  <div className="h-full bg-red-500 w-[45%] rounded-full"></div>
                </div>
              </div>

              {/* 보안 위협 */}
              <div className="flex flex-col gap-2">
                <span className="text-xs font-bold text-gray-500">최근 보안 위협</span>
                <div className="flex items-center gap-2">
                  <ShieldAlert size={20} className="text-gray-300" />
                  <h4 className="text-xl font-bold text-gray-900">None</h4>
                </div>
              </div>
            </div>
          </div>

        </section>
      </main>
    </div>
  );
};

export default UserAuthorization;