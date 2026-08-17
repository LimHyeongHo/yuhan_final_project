import React, { useState, useEffect } from 'react';
import { ArrowLeft, Users, Filter } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import Header from '../../../components/layout/Header';

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
    <div className="min-h-screen bg-gray-50 flex flex-col text-gray-900">
      <Header />
      
      <section className="bg-slate-900 text-white py-8 px-6 shadow-md">
        <div className="max-w-7xl mx-auto flex flex-col gap-2 relative">
          <button 
            onClick={() => navigate('/admin/dashboard')}
            className="absolute -top-4 left-0 flex items-center gap-1 text-sm text-slate-400 hover:text-white transition"
          >
            <ArrowLeft size={16} /> 대시보드로 돌아가기
          </button>
          <div className="flex items-center gap-3 mt-6">
            <div className="w-10 h-10 rounded-full bg-blue-500/20 flex items-center justify-center text-blue-400">
              <Users size={20} />
            </div>
            <h2 className="text-3xl font-extrabold tracking-tight">신규 가입자 통계 목록</h2>
          </div>
          <p className="text-slate-400 font-medium mt-1">최근 가입한 유저들의 활동 데이터 및 목록을 확인합니다.</p>
        </div>
      </section>

      <main className="flex-grow max-w-7xl w-full mx-auto p-6 md:p-8 flex flex-col gap-6">
        
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

        <div className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden">
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
  );
};

export default AdminUsersStatsPage;
