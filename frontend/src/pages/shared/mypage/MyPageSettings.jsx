import React, { useEffect, useState } from 'react';
import { Save, Lock, User, Mail, Calendar, Eye, EyeOff } from 'lucide-react';
import { validatePassword, validatePasswordConfirm } from '../../../utils/passwordValidation';

const MyPageSettings = () => {
  const [info, setInfo] = useState(null);
  const [nickname, setNickname] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [newPasswordConfirm, setNewPasswordConfirm] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [errors, setErrors] = useState({ password: '', passwordConfirm: '' });

  useEffect(() => {
    const fetchInfo = async () => {
      try {
        const res = await fetch('http://localhost:8080/api/member/info', {
          credentials: 'include',
        });
        const data = await res.json();
        if (res.ok) {
          setInfo(data);
          setNickname(data.nickname);
        } else {
          throw new Error(data.error || '회원 정보를 불러오지 못했습니다.');
        }
      } catch (e) {
        alert('회원 정보 조회 오류: ' + e.message);
      }
    };
    fetchInfo();
  }, []);

  const handlePasswordChange = (e) => {
    const value = e.target.value;
    setNewPassword(value);
    setErrors((prev) => ({
      ...prev,
      password: value ? validatePassword(value) : '',
      passwordConfirm: newPasswordConfirm ? validatePasswordConfirm(value, newPasswordConfirm) : '',
    }));
  };

  const handlePasswordConfirmChange = (e) => {
    const value = e.target.value;
    setNewPasswordConfirm(value);
    setErrors((prev) => ({ ...prev, passwordConfirm: validatePasswordConfirm(newPassword, value) }));
  };

  const handleSave = async (e) => {
    e.preventDefault();

    if (newPassword) {
      const passwordError = validatePassword(newPassword);
      const passwordConfirmError = validatePasswordConfirm(newPassword, newPasswordConfirm);
      if (passwordError || passwordConfirmError) {
        setErrors({ password: passwordError, passwordConfirm: passwordConfirmError });
        return;
      }
    }

    setIsSaving(true);
    try {
      const res = await fetch('http://localhost:8080/api/member/profile', {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ nickname, newPassword }),
      });
      const data = await res.json();
      if (res.ok) {
        localStorage.setItem('user_nickname', data.nickname);
        window.dispatchEvent(new Event('user-profile-updated')); // 새로고침 없이 헤더 등에 즉시 반영
        setNewPassword('');
        setNewPasswordConfirm('');
        setErrors({ password: '', passwordConfirm: '' });
        alert('변경사항이 저장되었습니다.');
      } else {
        throw new Error(data.error || '저장 중 오류가 발생했습니다.');
      }
    } catch (e) {
      alert('오류: ' + e.message);
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="bg-white rounded-[32px] p-8 border border-gray-200 shadow-sm flex flex-col gap-8">
      <div className="pb-6 border-b border-gray-100">
        <h4 className="text-lg font-bold text-gray-900">기본 정보 설정</h4>
        <p className="text-xs text-gray-400 font-medium mt-1">프로필 정보와 안전한 계정 관리를 위한 비밀번호를 변경할 수 있습니다.</p>
      </div>

      <form className="flex flex-col gap-6" onSubmit={handleSave}>

        {/* 수정 불가 영역 (Read-only) */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 p-5 bg-gray-50 rounded-2xl border border-gray-100">
          <div className="flex flex-col gap-2">
            <label className="text-xs font-bold text-gray-500 flex items-center gap-1.5"><Mail size={14}/> 이메일 계정</label>
            <input type="text" value={info?.email ?? ''} disabled className="px-4 py-2.5 bg-gray-100 text-gray-500 rounded-xl text-sm font-semibold border-none outline-none" />
          </div>
          <div className="flex flex-col gap-2">
            <label className="text-xs font-bold text-gray-500 flex items-center gap-1.5"><Calendar size={14}/> 가입 일시</label>
            <input type="text" value={info?.createdAt ?? ''} disabled className="px-4 py-2.5 bg-gray-100 text-gray-500 rounded-xl text-sm font-semibold border-none outline-none" />
          </div>
        </div>

        {/* 수정 가능 영역 */}
        <div className="flex flex-col gap-6 px-2 mt-2">
          <div className="flex flex-col gap-2">
            <label className="text-xs font-bold text-gray-700 flex items-center gap-1.5"><User size={14}/> 닉네임 (이름)</label>
            <input
              type="text"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              className="px-4 py-2.5 bg-white border border-gray-200 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 rounded-xl text-sm font-semibold outline-none transition"
            />
          </div>

          <div className="flex flex-col gap-2">
            <label className="text-xs font-bold text-gray-700 flex items-center gap-1.5"><Lock size={14}/> 새 비밀번호</label>
            <div className="relative">
              <input
                type={showPassword ? 'text' : 'password'}
                value={newPassword}
                onChange={handlePasswordChange}
                placeholder="변경할 비밀번호를 입력하세요 (변경 안 하려면 비워두세요)"
                className={`w-full px-4 py-2.5 pr-11 bg-white border ${errors.password ? 'border-red-400' : 'border-gray-200'} focus:border-blue-500 focus:ring-2 focus:ring-blue-100 rounded-xl text-sm font-semibold outline-none transition`}
              />
              <button
                type="button"
                onClick={() => setShowPassword((prev) => !prev)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition"
                aria-label={showPassword ? '비밀번호 숨기기' : '비밀번호 표시'}
              >
                {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
            {errors.password && <p className="text-xs text-red-500 font-semibold">{errors.password}</p>}
          </div>

          <div className="flex flex-col gap-2">
            <label className="text-xs font-bold text-gray-700 flex items-center gap-1.5"><Lock size={14}/> 새 비밀번호 확인</label>
            <input
              type={showPassword ? 'text' : 'password'}
              value={newPasswordConfirm}
              onChange={handlePasswordConfirmChange}
              placeholder="새 비밀번호를 다시 입력하세요"
              className={`w-full px-4 py-2.5 bg-white border ${errors.passwordConfirm ? 'border-red-400' : 'border-gray-200'} focus:border-blue-500 focus:ring-2 focus:ring-blue-100 rounded-xl text-sm font-semibold outline-none transition`}
            />
            {errors.passwordConfirm && <p className="text-xs text-red-500 font-semibold">{errors.passwordConfirm}</p>}
          </div>
        </div>

        <div className="flex justify-end pt-6 border-t border-gray-100 mt-2">
          <button type="submit" disabled={isSaving} className="px-6 py-3 bg-blue-600 hover:bg-blue-700 text-white text-sm font-bold rounded-xl transition flex items-center gap-2 shadow-md disabled:opacity-50">
            <Save size={16} /> {isSaving ? '저장 중...' : '변경사항 저장'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default MyPageSettings;
