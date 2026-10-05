import React, { useState, useEffect } from 'react';
import { ArrowLeft, Users, Filter } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import AdminHeader from '../../../components/admin/AdminHeader';

const AdminUsersStatsPage = () => {
  const navigate = useNavigate();
  const [users, setUsers] = useState([]);
  const [days, setDays] = useState(1);

  useEffect(() => {
    fetch(`http://localhost:8080/api/admin/users/stats-list?days=${days}`, { credentials: 'include' })
      .then(res => res.json())
      .then(data => setUsers(data))
      .catch(err => console.error("데이터 로드 실패:", err));
  }, [days]);

  return (
    <div className="v3-page admin-v3-page min-h-screen flex flex-col text-gray-900">
      <AdminHeader />
      
      <div className="v3-unified-page-card">
        <header className="v3-unified-page-card__header">
          <h1 className="v3-unified-page-card__title"><Users size={28} />신규 가입자 통계 목록</h1>
          <button 
            onClick={() => navigate('/admin/dashboard')}
            className="flex items-center gap-1 text-sm font-bold text-gray-600 transition hover:text-gray-900"
          >
            <ArrowLeft size={16} /> 대시보드로 돌아가기
          </button>
        </header>

        <main className="v3-unified-page-card__body admin-v3-main flex-grow">
        
        <div className="flex justify-between items-end">
          <h3 className="text-xl font-bold tracking-tight">가입자 데이터 조회</h3>
          <div className="flex items-center gap-2 bg-white border border-gray-200 rounded-lg p-1 shadow-sm">
            <Filter size={14} className="text-gray-400 ml-2" />
            <button 
              onClick={() => setDays(1)}
              className={`px-4 py-1.5 text-sm font-bold rounded-md transition ${days === 1 ? 'bg-blue-50 text-blue-600' : 'text-gray-500 hover:bg-gray-50'}`}
            >
              오늘 (1일)
            </button>
            <button 
              onClick={() => setDays(7)}
              className={`px-4 py-1.5 text-sm font-bold rounded-md transition ${days === 7 ? 'bg-blue-50 text-blue-600' : 'text-gray-500 hover:bg-gray-50'}`}
            >
              1주일 (7일)
            </button>
            <button 
              onClick={() => setDays(30)}
              className={`px-4 py-1.5 text-sm font-bold rounded-md transition ${days === 30 ? 'bg-blue-50 text-blue-600' : 'text-gray-500 hover:bg-gray-50'}`}
            >
              1개월 (30일)
            </button>
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200 text-gray-500 text-sm">
                <th className="py-4 px-6 font-bold">아이디 (이메일)</th>
                <th className="py-4 px-6 font-bold">닉네임</th>
                <th className="py-4 px-6 font-bold text-right">가입 일자</th>
              </tr>
            </thead>
            <tbody>
              {users.length === 0 ? (
                <tr>
                  <td colSpan="3" className="py-8 text-center text-gray-400 font-bold">해당 기간에 가입한 유저가 없습니다.</td>
                </tr>
              ) : (
                users.map((user, idx) => (
                  <tr key={idx} className="border-b border-gray-50 hover:bg-gray-50/50 transition">
                    <td className="py-4 px-6 text-gray-900 font-semibold">{user.email}</td>
                    <td className="py-4 px-6 text-gray-600">{user.nickname}</td>
                    <td className="py-4 px-6 text-gray-500 text-right">{user.createdAt}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        </main>
      </div>
    </div>
  );
};

export default AdminUsersStatsPage;
