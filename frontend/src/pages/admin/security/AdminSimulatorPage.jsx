import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../../../components/layout/Header';
import { Shield, AlertTriangle, CheckCircle, Clock, ServerCrash } from 'lucide-react';

const AdminSimulatorPage = () => {
  const navigate = useNavigate();
  const [verifyResult, setVerifyResult] = useState(null);
  const [simulating, setSimulating] = useState(false);
  const [currentStep, setCurrentStep] = useState(0); // 0: Idle, 1: Hacking, 2: Verifying, 3: Restoring, 4: Finished
  const [ganacheError, setGanacheError] = useState(false);
  const [logs, setLogs] = useState([]);
  const [hackDetails, setHackDetails] = useState(null);

  const addLog = (msg) => {
    setLogs(prev => [...prev, msg]);
  };

  const runSimulation = async () => {
    if (!window.confirm("DB 해킹 ➔ 블록체인 검증 ➔ 원상 복구 과정을 자동으로 시연합니다.\n계속하시겠습니까?")) return;

    setSimulating(true);
    setCurrentStep(1);
    setVerifyResult(null);
    setGanacheError(false);
    setLogs([]);

    let hackedId = null;

    // --- STEP 1: 해킹 ---
    try {
      // 1. API 호출을 먼저 해서 데이터를 받아옵니다.
      const res = await fetch('http://localhost:8080/api/admin/security/simulate-hack', {
        method: 'POST', credentials: 'include'
      });
      if (!res.ok) throw new Error("해킹 시뮬레이션 요청 실패");

      const data = await res.json();
      hackedId = data.productId;

      // 2. 데이터가 준비되면 '1단계 시작' 로그와 'DB 변조 내역(오른쪽 패널)'을 동시에 띄웁니다.
      addLog("▶ 1단계: 타겟 상품 DB 강제 조작 중...");
      setHackDetails(data);

      // 3. 조작 중이라는 느낌을 주기 위해 3초 대기
      await new Promise(r => setTimeout(r, 300));

      // 4. 해킹 완료 로그 출력
      addLog(`✅ [해킹 완료] 상품 ID ${hackedId}의 가격이 백엔드 단에서 몰래 +10,000원 변조되었습니다.`);
    } catch (e) {
      addLog(`❌ [오류] ${e.message}`);
      setSimulating(false);
      setCurrentStep(0);
      return;
    }

    // 5. 1단계 완료 후 2단계가 나오기 전까지 사용자가 읽을 시간(2.5초) 부여
    await new Promise(r => setTimeout(r, 2500));

    // --- STEP 2: 검증 ---
    setCurrentStep(2);
    let shouldRestore = false;
    try {
      addLog("▶ 2단계: 실시간 블록체인 해시 무결성 검증 엔진 가동...");
      const res = await fetch(`http://localhost:8080/api/products/${hackedId}/verify`, {
        credentials: 'include'
      });
      if (!res.ok) throw new Error("검증 API 요청 실패");

      const data = await res.json();
      setVerifyResult(data);

      if (data.status === 'PENDING') {
        // 노드가 꺼져있거나 조회가 안될 때
        setGanacheError(true);
        addLog("❌ [치명적 오류] 블록체인 네트워크(Sepolia/로컬) 응답 없음 또는 데이터 없음!");
        addLog("   - 원인: 노드 연결 끊김 또는 해당 상품이 스마트 컨트랙트에 없습니다.");
        addLog("   - 조치: 새 상품을 등록하여 트랜잭션을 발생시킨 후 다시 시도해주세요.");
        shouldRestore = true;
      } else {
        // 가나슈가 켜져있어 검증 성공 시
        addLog(`✅ [검증 완료] 불일치 적발! 상태: ${data.status}`);
        shouldRestore = true;
      }
    } catch (e) {
      addLog(`❌ [오류] ${e.message}`);
      shouldRestore = true;
    }

    // 시각적 딜레이 (사용자가 결과를 볼 시간)
    await new Promise(r => setTimeout(r, 3000));

    // --- STEP 3: 복구 ---
    if (shouldRestore && hackedId) {
      setCurrentStep(3);
      addLog("▶ 3단계: 변조된 데이터베이스 원상 복구(Rollback) 진행 중...");
      try {
        const res = await fetch(`http://localhost:8080/api/admin/security/restore/${hackedId}`, {
          method: 'POST', credentials: 'include'
        });
        if (res.ok) {
          addLog("✅ [복구 완료] 데이터가 안전하게 정상 수치로 롤백되었습니다.");
        } else {
          addLog("❌ [복구 실패] 데이터 롤백 실패. 수동 확인 요망.");
        }
      } catch (e) {
        addLog(`❌ [복구 오류] ${e.message}`);
      }
    }

    setCurrentStep(4);
    setSimulating(false);
  };

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <Header />

      {/* 상단 다크 배너 */}
      <section className="bg-slate-900 text-white py-12 px-6 shadow-md">
        <div className="max-w-[1600px] mx-auto flex flex-col gap-2">
          <span className="bg-red-500/20 text-red-400 text-xs font-bold uppercase tracking-widest px-3 py-1 rounded-full w-max border border-red-500/30">
            Hacker Simulator (Automated)
          </span>
          <h2 className="text-4xl font-extrabold tracking-tight">
            보안 검증 시뮬레이터
          </h2>
          <p className="text-slate-400 font-medium text-base max-w-2xl">
            단 한 번의 클릭으로 DB 해킹, 블록체인 검증 적발, 데이터 롤백까지의 모든 보안 시나리오를 자동으로 시연합니다.
          </p>
        </div>
      </section>

      <main className="flex-grow max-w-[1600px] w-full mx-auto p-6 md:p-8 flex flex-col gap-8">

        <button onClick={() => navigate('/admin/security')} className="text-sm text-blue-600 hover:underline flex items-center gap-1 w-max">
          ← 보안 로그로 돌아가기
        </button>

        {/* 시뮬레이션 컨트롤 패널 */}
        <div className="bg-white rounded-[24px] p-8 border border-gray-200 shadow-sm flex flex-col md:flex-row gap-8 items-center justify-between">
          <div className="flex flex-col gap-2 flex-1">
            <h3 className="text-xl font-bold text-slate-800">원클릭 시연 시퀀스</h3>
            <p className="text-sm text-slate-500 leading-relaxed">
              1. 임의의 상품 가격을 DB에서 고의로 +10,000원 변조합니다.<br />
              2. 블록체인 검증 엔진을 호출하여 해시 불일치를 적발합니다.<br />
              3. 시연 종료 후 변조된 가격을 즉시 원상 복구합니다.
            </p>
          </div>

          <button
            onClick={runSimulation}
            disabled={simulating}
            className={`px-8 py-4 rounded-2xl font-black text-lg text-white transition-all shadow-xl flex items-center gap-3 ${simulating ? 'bg-gray-400 cursor-wait' : 'bg-red-600 hover:bg-red-700 hover:scale-105 active:scale-95'
              }`}
          >
            {simulating ? <Clock className="animate-spin" size={24} /> : <AlertTriangle size={24} />}
            {simulating ? "시뮬레이션 진행 중..." : "▶ 시뮬레이션 전과정 시작"}
          </button>
        </div>

        {/* 시뮬레이션 결과 및 로그 패널 */}
        {(currentStep > 0) && (
          <div className="bg-slate-900 rounded-[24px] p-8 shadow-2xl text-white border border-slate-700 overflow-hidden relative">

            {/* 가나슈 오류 시 강력한 경고 배경 이펙트 */}
            {ganacheError && (
              <div className="absolute inset-0 bg-red-600/10 animate-pulse pointer-events-none"></div>
            )}

            <h4 className="text-lg font-bold mb-6 flex items-center gap-2 border-b border-slate-700 pb-4">
              <span className={simulating ? "text-blue-400 animate-pulse" : "text-green-400"}>●</span>
              시스템 라이브 로그
            </h4>

            <div className="flex flex-col lg:flex-row gap-6">

              {/* 왼쪽: 시스템 라이브 로그 */}
              <div className="flex-1 flex flex-col gap-3 font-mono text-sm">
                {logs.map((log, idx) => (
                  <div key={idx} className={`p-3 rounded-lg border-l-4 ${log.includes("❌") ? 'border-red-500 bg-red-900/20 text-red-200' :
                    log.includes("▶") ? 'border-blue-500 bg-blue-900/20 text-blue-200' :
                      'border-green-500 bg-green-900/20 text-green-200'
                    }`}>
                    {log}
                  </div>
                ))}
              </div>

              {/* 오른쪽: 조작된 데이터 상세 (해킹 완료 시 표시) */}
              {hackDetails && (
                <div className="w-full lg:w-[720px] flex-shrink-0 bg-slate-800 rounded-xl p-6 font-mono text-sm border border-slate-600 h-max shadow-xl">
                  <h5 className="text-red-400 font-bold mb-4 flex items-center gap-2 border-b border-slate-700 pb-2">
                    <AlertTriangle size={18} /> DB 데이터 변조 상세내역
                  </h5>
                  <div className="flex flex-col gap-4">
                    <div className="bg-slate-900 p-4 rounded-lg flex flex-col gap-2 border border-slate-700">
                      <span className="text-slate-400 font-bold border-b border-slate-800 pb-1">기존 정상 데이터</span>
                      <div><span className="text-slate-500">DB ID:</span> <span className="text-blue-300">{hackDetails.productId}</span></div>
                      <div><span className="text-slate-500">가격:</span> <span className="text-blue-300">{hackDetails.originalPrice?.toLocaleString()}원</span></div>
                      <div className="break-all"><span className="text-slate-500">정상 해시:</span> <span className="text-blue-300">{hackDetails.originalHash}</span></div>
                    </div>

                    <div className="flex justify-center text-red-500">
                      ↓ 실시간 변조 발생 ↓
                    </div>

                    <div className="bg-slate-900 p-4 rounded-lg flex flex-col gap-2 border border-red-500/50 relative overflow-hidden">
                      <div className="absolute top-0 right-0 bg-red-600 text-white text-xs px-2 py-1 font-bold rounded-bl-lg">HACKED</div>
                      <span className="text-red-400 font-bold border-b border-slate-800 pb-1">해킹 변조 후 데이터</span>
                      <div><span className="text-slate-500">DB ID:</span> <span className="text-red-300">{hackDetails.productId}</span></div>
                      <div><span className="text-slate-500">가격:</span> <span className="text-red-400">{hackDetails.newPrice?.toLocaleString()}원 <span className="text-xs text-red-500/80">(+10,000)</span></span></div>
                      <div className="break-all"><span className="text-slate-500">변조 해시:</span> <span className="text-red-400">{hackDetails.newHash}</span></div>
                    </div>
                  </div>
                </div>
              )}
            </div>

            {ganacheError && (
              <div className="mt-8 bg-red-950/50 border border-red-800 rounded-xl p-6 flex items-start gap-4 text-red-200">
                <ServerCrash className="text-red-500 flex-shrink-0" size={32} />
                <div className="flex flex-col gap-2">
                  <h5 className="font-bold text-red-400 text-lg">블록체인 노드 연결 실패 / 데이터 없음</h5>
                  <p className="text-sm leading-relaxed">
                    블록체인 노드가 응답하지 않거나 해당 상품의 기록이 없어 무결성 검증을 완료할 수 없습니다.<br />
                    새로운 환경(Sepolia 등)에 연결하셨다면, 새로운 상품을 1개 이상 추가 등록하신 뒤 시도해 주세요.
                  </p>
                </div>
              </div>
            )}

            {/* 검증 결과 (정상 적발) */}
            {verifyResult && !ganacheError && (
              <div className="mt-8 bg-slate-800 rounded-xl p-6 font-mono text-sm break-all flex flex-col gap-4 border border-slate-600">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="bg-slate-900 p-4 rounded-lg">
                    <span className="text-slate-400 block mb-1">Target DB Data (조작됨):</span>
                    <span className="text-blue-300 font-bold">{verifyResult.targetData}</span>
                  </div>
                  <div className="bg-slate-900 p-4 rounded-lg">
                    <span className="text-slate-400 block mb-1">Computed DB Hash:</span>
                    <span className="text-red-400 font-bold">{verifyResult.dbHash}</span>
                  </div>
                </div>

                <div className="bg-slate-900 p-4 rounded-lg">
                  <span className="text-slate-400 block mb-1">Stored Blockchain Hash (원본):</span>
                  <span className="text-green-400 font-bold">{verifyResult.blockchainHash}</span>
                </div>

                <div className="mt-4 pt-6 border-t border-slate-700 flex flex-col md:flex-row items-center justify-center gap-4">
                  <span className="text-slate-400">최종 판정 결과:</span>
                  <span className={`font-black text-base px-6 py-2 rounded-lg ${verifyResult.status === 'FORGED' ? 'bg-red-500/20 text-red-500' : 'bg-green-500/20 text-green-500'}`}>
                    [{verifyResult.status}] {verifyResult.message}
                  </span>
                </div>
              </div>
            )}

            {/* 시뮬레이션 종료 시 뱃지/버튼 */}
            {currentStep === 4 && (
              <div className="mt-8 flex justify-center">
                <button
                  onClick={() => navigate('/admin/security')}
                  className="inline-flex items-center gap-2 bg-slate-800 hover:bg-slate-700 transition-colors text-slate-300 px-6 py-3 rounded-full text-sm font-bold shadow-lg border border-slate-600"
                >
                  <CheckCircle size={18} className="text-green-500" /> 시연 시퀀스 종료 및 보안 로그로 돌아가기
                </button>
              </div>
            )}
          </div>
        )}

      </main>
    </div>
  );
};

export default AdminSimulatorPage;
