import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../../../components/layout/Header';
import { Shield, AlertTriangle, CheckCircle, Clock, ServerCrash, RefreshCw, Database } from 'lucide-react';

const AdminSimulatorPage = () => {
  const navigate = useNavigate();
  const [verifyResult, setVerifyResult] = useState(null);
  const [simulating, setSimulating] = useState(false);
  const [currentStep, setCurrentStep] = useState(0); // 0: Idle, 1: Hacking, 2: Verifying, 3: Restoring, 4: Finished
  const [ganacheError, setGanacheError] = useState(false);
  const [logs, setLogs] = useState([]);
  const [hackedId, setHackedId] = useState(null);
  const [canRestore, setCanRestore] = useState(false);
  const [hackDetails, setHackDetails] = useState(null);

  // 세션 스토리지에서 상태 복구 (다른 페이지 다녀와도 복구 버튼 유지)
  React.useEffect(() => {
    const saved = sessionStorage.getItem('adminSimulatorState');
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        if (parsed.canRestore) {
          setCurrentStep(parsed.currentStep);
          setGanacheError(parsed.ganacheError);
          setLogs(parsed.logs || []);
          setHackedId(parsed.hackedId);
          setCanRestore(parsed.canRestore);
          setHackDetails(parsed.hackDetails);
          setVerifyResult(parsed.verifyResult);
        }
      } catch (e) {
        console.error("Failed to parse saved simulator state");
      }
    }
  }, []);

  // 상태 변경 시 세션 스토리지에 저장
  React.useEffect(() => {
    if (currentStep > 0 && currentStep < 4) {
      sessionStorage.setItem('adminSimulatorState', JSON.stringify({
        currentStep, ganacheError, logs, hackedId, canRestore, hackDetails, verifyResult
      }));
    } else if (currentStep === 4 || currentStep === 0) {
      sessionStorage.removeItem('adminSimulatorState');
    }
  }, [currentStep, ganacheError, logs, hackedId, canRestore, hackDetails, verifyResult]);

  const addLog = (msg) => {
    setLogs(prev => [...prev, msg]);
  };

  const runSimulation = async () => {
    if (!window.confirm("DB 해킹 ➔ 블록체인 검증 과정을 진행합니다.\n계속하시겠습니까?")) return;

    setSimulating(true);
    setCurrentStep(1);
    setVerifyResult(null);
    setGanacheError(false);
    setLogs([]);
    setCanRestore(false);
    setHackedId(null);

    let currentHackedId = null;

    // --- STEP 1: 해킹 ---
    try {
      const res = await fetch('http://localhost:8080/api/admin/security/simulate-hack', {
        method: 'POST', credentials: 'include'
      });
      if (!res.ok) throw new Error("해킹 시뮬레이션 요청 실패");

      const data = await res.json();
      currentHackedId = data.productId;
      setHackedId(currentHackedId);

      addLog("▶ 1단계: 전체 상품 중 무작위(Random) 타겟 상품 선정 및 DB 강제 조작 중...");
      setHackDetails(data);

      await new Promise(r => setTimeout(r, 300));
      addLog(`✅ [해킹 완료] 상품 ID ${currentHackedId}의 가격이 백엔드 단에서 몰래 999,999원으로 변조되었습니다.`);
    } catch (e) {
      addLog(`❌ [오류] ${e.message}`);
      setSimulating(false);
      setCurrentStep(0);
      return;
    }

    await new Promise(r => setTimeout(r, 2500));

    // --- STEP 2: 검증 ---
    setCurrentStep(2);
    try {
      addLog("▶ 2단계: 실시간 블록체인 해시 무결성 검증 엔진 가동...");
      const res = await fetch(`http://localhost:8080/api/products/${currentHackedId}/verify`, {
        credentials: 'include'
      });
      if (!res.ok) throw new Error("검증 API 요청 실패");

      const data = await res.json();
      setVerifyResult(data);

      if (data.status === 'PENDING') {
        setGanacheError(true);
        addLog("⚠️ [검증 보류] 블록체인 기록을 현재 조회할 수 없습니다.");
        addLog("   - 원인: 네트워크 오류 또는 트랜잭션 미확정 상태일 수 있습니다.");
        addLog("   - 조치: 잠시 후 다시 시도하거나 레거시 마이그레이션 결과를 확인해주세요.");
      } else if (data.status === 'FORGED') {
        addLog(`✅ [검증 완료] 불일치 적발! 상태: ${data.status}`);
      } else {
        addLog(`⚠️ [예상 외 결과] 변조 데이터가 ${data.status} 상태로 판정되었습니다.`);
      }
    } catch (e) {
      addLog(`❌ [오류] ${e.message}`);
    }

    setCanRestore(true);
    setSimulating(false);
  };

  const runRestore = async () => {
    if (!hackedId) return;
    
    setCurrentStep(3);
    setSimulating(true);
    addLog("▶ 3단계: 변조된 데이터베이스 원상 복구(Rollback) 진행 중...");
    try {
      const res = await fetch(`http://localhost:8080/api/admin/security/restore/${hackedId}`, {
        method: 'POST', credentials: 'include'
      });
      if (res.ok) {
        addLog("✅ [복구 완료] 데이터가 안전하게 원본 가격으로 롤백되었습니다.");
      } else {
        addLog("❌ [복구 실패] 데이터 롤백 실패. 수동 확인 요망.");
      }
    } catch (e) {
      addLog(`❌ [복구 오류] ${e.message}`);
    }
    
    setCurrentStep(4);
    setSimulating(false);
  };

  const [migrating, setMigrating] = useState(false);
  const [migrationProgress, setMigrationProgress] = useState(null);
  const runMigration = async () => {
    if (!window.confirm("과거 레거시 데이터를 블록체인 네트워크로 마이그레이션(동기화) 하시겠습니까?\n\n데이터 양에 따라 수십 초가 소요될 수 있습니다.")) return;

    setMigrating(true);
    setCurrentStep(5);
    setLogs([]);
    setMigrationProgress(null);
    addLog("▶ 마이그레이션 시작: 온체인 해시가 없는 레거시 데이터를 조회합니다...");
    addLog("▶ 작업은 백그라운드에서 실행되며 진행률을 자동으로 조회합니다...");

    try {
      const res = await fetch(`http://localhost:8080/api/admin/security/migrate-legacy`, {
        method: 'POST', credentials: 'include'
      });
      if (!res.ok) throw new Error("마이그레이션 작업 시작 실패");

      let data = await res.json();
      if (!data.jobId) throw new Error("마이그레이션 작업 ID를 받지 못했습니다.");
      setMigrationProgress(data);

      while (data.status === 'QUEUED' || data.status === 'RUNNING') {
        await new Promise(resolve => setTimeout(resolve, 1500));
        const statusRes = await fetch(
          `http://localhost:8080/api/admin/security/migrate-legacy/${data.jobId}`,
          { credentials: 'include' }
        );
        if (!statusRes.ok) throw new Error("마이그레이션 진행 상태 조회 실패");
        data = await statusRes.json();
        setMigrationProgress(data);
      }

        const confirmed = data.confirmedCount ?? data.count ?? 0;
        const failed = data.failedCount ?? 0;
        const alreadySynced = data.alreadySyncedCount ?? 0;
        const remediated = data.remediatedCount ?? 0;
        const mismatched = data.mismatchCount ?? 0;

        addLog(`✅ [검사 완료] 확정 ${confirmed}개(구형 규격 교정 ${remediated}개) / 기존 정상 ${alreadySynced}개 / 실패 ${failed}개 / 해시 불일치 ${mismatched}개`);

        if (confirmed > 0) {
          addLog(`✅ [확정 완료] ${confirmed}개의 레거시 데이터가 Sepolia에 기록되고 검증되었습니다.`);
        }
        if (mismatched > 0) {
          addLog(`⚠️ [보호 조치] ${mismatched}개 상품은 기존 온체인 해시와 달라 자동 덮어쓰지 않았습니다.`);
        }
        if (failed > 0) {
          addLog(`❌ [일부 실패] ${failed}개 상품의 트랜잭션 확정을 완료하지 못했습니다.`);
        }

        (data.items || [])
          .filter(item => item.status !== 'CONFIRMED')
          .forEach(item => addLog(
            `   - 상품 ${item.productId}: ${item.status} / ${item.message}`
          ));

        if (data.status === 'SUCCESS' && confirmed === 0) {
          addLog(`✅ [완료] 새로 동기화할 데이터가 없습니다. 기존 ${alreadySynced}개 데이터가 정상입니다.`);
        } else if (data.status === 'SUCCESS') {
          addLog(`✅ [완료] 모든 마이그레이션 트랜잭션이 확정되었습니다.`);
        } else if (data.status === 'PARTIAL_SUCCESS') {
          addLog(`⚠️ [부분 완료] 실패 또는 불일치 항목을 확인해주세요.`);
        } else {
          addLog(`❌ [실패] 확정된 마이그레이션이 없습니다. 상세 결과를 확인해주세요.`);
        }
    } catch (e) {
      addLog(`❌ [네트워크 오류] ${e.message}`);
    } finally {
      setMigrating(false);
    }
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
              1. 등록된 전체 상품 중 <strong>무작위(Random)로 1개</strong>의 상품을 골라 고의로 999,999원으로 변조합니다.<br />
              2. 블록체인 검증 엔진을 호출하여 해시 불일치를 적발합니다.<br />
              3. 이후 수동으로 "복구하기" 버튼을 눌러 원본 가격으로 롤백합니다.
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

        {/* 레거시 마이그레이션 패널 */}
        <div className="bg-white rounded-[24px] p-8 border border-gray-200 shadow-sm flex flex-col md:flex-row gap-8 items-center justify-between">
          <div className="flex flex-col gap-2 flex-1">
            <h3 className="text-xl font-bold text-slate-800 flex items-center gap-2">
              <Database className="text-blue-500" size={24} />
              레거시 데이터 마이그레이션
            </h3>
            <p className="text-sm text-slate-500 leading-relaxed">
              블록체인 보안 시스템 도입 이전에 등록되어 <strong>온체인 해시가 없는 과거 상품 데이터</strong>를 모두 찾아<br />
              트랜잭션 영수증과 저장된 해시를 확인한 뒤 동기화 완료로 판정합니다.
            </p>
            {migrationProgress && (
              <div className="mt-3 max-w-xl">
                <div className="flex justify-between text-xs font-bold text-slate-600 mb-1">
                  <span>
                    {migrationProgress.status === 'QUEUED' ? '대기 중' :
                      migrationProgress.status === 'RUNNING' ? `상품 ${migrationProgress.currentProductId ?? '-'} 처리 중` :
                        '처리 완료'}
                  </span>
                  <span>{migrationProgress.processedCount ?? 0} / {migrationProgress.totalCount ?? 0}</span>
                </div>
                <div className="h-2.5 rounded-full bg-slate-200 overflow-hidden">
                  <div
                    className="h-full bg-blue-600 transition-all duration-500"
                    style={{ width: `${migrationProgress.progressPercent ?? 0}%` }}
                  />
                </div>
                <p className="text-xs text-slate-500 mt-1">
                  확정 {migrationProgress.confirmedCount ?? 0} · 기존 정상 {migrationProgress.alreadySyncedCount ?? 0} · 실패 {migrationProgress.failedCount ?? 0}
                </p>
              </div>
            )}
          </div>

          <button
            onClick={runMigration}
            disabled={migrating || simulating}
            className={`px-8 py-4 rounded-2xl font-black text-lg text-white transition-all shadow-xl flex items-center gap-3 ${
              (migrating || simulating) ? 'bg-gray-400 cursor-wait' : 'bg-blue-600 hover:bg-blue-700 hover:scale-105 active:scale-95'
            }`}
          >
            {migrating ? <Clock className="animate-spin" size={24} /> : <RefreshCw size={24} />}
            {migrating ? "마이그레이션 진행 중..." : "▶ 레거시 데이터 동기화"}
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
                      <div><span className="text-slate-500">가격:</span> <span className="text-red-400">{hackDetails.newPrice?.toLocaleString()}원 <span className="text-xs text-red-500/80">(조작됨)</span></span></div>
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
                  <h5 className="font-bold text-red-400 text-lg">블록체인 기록 조회 보류</h5>
                  <p className="text-sm leading-relaxed">
                    네트워크 오류 또는 아직 확정되지 않은 트랜잭션 때문에 무결성 검증을 완료할 수 없습니다.<br />
                    잠시 후 다시 시도하거나 레거시 마이그레이션의 항목별 결과를 확인해 주세요.
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

            {/* 복구 버튼 또는 시퀀스 종료 버튼 */}
            {canRestore && currentStep === 2 && (
              <div className="mt-8 flex justify-center">
                <button
                  onClick={runRestore}
                  disabled={simulating}
                  className="inline-flex items-center gap-2 bg-blue-600 hover:bg-blue-700 transition-colors text-white px-8 py-4 rounded-xl text-base font-bold shadow-lg"
                >
                  <RefreshCw size={20} className={simulating ? "animate-spin" : ""} /> 
                  데이터 원상 복구하기 (Rollback)
                </button>
              </div>
            )}

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

            {currentStep === 5 && !migrating && (
              <div className="mt-8 flex justify-center">
                <button
                  onClick={() => {
                    setCurrentStep(0);
                    setLogs([]);
                  }}
                  className="inline-flex items-center gap-2 bg-slate-800 hover:bg-slate-700 transition-colors text-slate-300 px-6 py-3 rounded-full text-sm font-bold shadow-lg border border-slate-600"
                >
                  <CheckCircle size={18} className="text-green-500" /> 확인 (창 닫기)
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
