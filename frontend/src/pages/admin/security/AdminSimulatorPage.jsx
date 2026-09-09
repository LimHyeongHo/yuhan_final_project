import React, { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../../../components/layout/Header';
import {
  AlertTriangle,
  CheckCircle,
  Clock,
  ChevronDown,
  Database,
  RefreshCw,
  ServerCrash,
} from 'lucide-react';

const API = 'http://localhost:8080/api/admin/security';
const SIMULATION_ID_KEY = 'adminSimulatorSimulationId';
const PRODUCTS_PER_PAGE = 10;
const DEMO_GENESIS_HASH = '0'.repeat(64);
const MAX_TAMPER_PRICE = 5000000;

const statusText = {
  FORGED_DETECTED: '상품 정보 위변조',
  VERIFIED: '기본값으로 복구',
  RESTORED: 'DB 값이 복구되었습니다. 블록체인 재검증 중입니다',
  VERIFICATION_UNAVAILABLE: '블록체인 원본을 지금 조회할 수 없습니다',
  HASH_MISMATCH: '복구 후에도 원본 지문과 일치하지 않습니다',
  RECOVERY_REQUIRED: '정상 변경 또는 복구 충돌을 확인해야 합니다',
};

const getErrorMessage = async (response) => {
  try {
    const body = await response.json();
    return body.message || body.error || `요청에 실패했습니다 (${response.status})`;
  } catch (e) {
    return `요청에 실패했습니다 (${response.status})`;
  }
};

const shortHash = (hash) => {
  if (!hash) return '조회할 수 없음';
  const value = String(hash);
  return value.length > 22 ? `${value.slice(0, 12)}…${value.slice(-8)}` : value;
};

const sha256Hex = async (value) => {
  const buffer = await window.crypto.subtle.digest('SHA-256', new TextEncoder().encode(value));
  return Array.from(new Uint8Array(buffer), (byte) => byte.toString(16).padStart(2, '0')).join('');
};

const createDemoChain = async (product) => {
  const price = Number(product?.price || 0);
  const events = [
    { type: 'PRODUCT_CREATED', title: '상품 등록', detail: `상품 #${product.productId} · ${price.toLocaleString()}원` },
    { type: 'PRICE_RECORDED', title: '가격 원본 기록', detail: `기준 가격 ${price.toLocaleString()}원` },
    { type: 'PRICE_VERSION', title: '가격 버전 1', detail: `거래 가격 ${price.toLocaleString()}원` },
    { type: 'PARTICIPATION', title: '참여 기록', detail: '참여 인원 3명' },
    { type: 'PARTICIPATION', title: '참여 기록', detail: '참여 인원 10명' },
  ];
  let previousHash = DEMO_GENESIS_HASH;
  const nodes = [];

  for (const [index, event] of events.entries()) {
    const payload = JSON.stringify({ productId: product.productId, sequence: index + 1, ...event });
    const hash = await sha256Hex(`${previousHash}|${payload}`);
    nodes.push({
      sequence: index + 1,
      ...event,
      payload,
      previousHash,
      storedHash: hash,
      expectedHash: hash,
      hashValid: true,
      linkValid: true,
      valid: true,
    });
    previousHash = hash;
  }
  return nodes;
};

const validateDemoChain = async (nodes) => {
  let previousHash = DEMO_GENESIS_HASH;
  let previousValid = true;
  const validated = [];

  for (const node of nodes) {
    const expectedHash = await sha256Hex(`${node.previousHash}|${node.payload}`);
    const hashValid = expectedHash === node.storedHash;
    const linkValid = node.previousHash === previousHash;
    const valid = previousValid && hashValid && linkValid;
    validated.push({ ...node, expectedHash, hashValid, linkValid, valid });
    previousHash = expectedHash;
    previousValid = valid;
  }
  return validated;
};

const DemoHashChain = ({ chain, product, loading, processing, progress, tamperPrice, onTamperPriceChange, onCreate, onTamper, onRestore }) => {
  const middleIndex = chain.findIndex((node) => node.type === 'PRICE_VERSION') >= 0
    ? chain.findIndex((node) => node.type === 'PRICE_VERSION')
    : Math.floor(chain.length / 2);
  const invalidCount = chain.filter((node) => !node.valid).length;
  const tampered = chain.some((node) => !node.hashValid);
  const enteredPrice = Number(String(tamperPrice).replace(/,/g, '').trim());
  const priceOverLimit = tamperPrice !== '' && Number.isFinite(enteredPrice) && enteredPrice > MAX_TAMPER_PRICE;

  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm md:p-8">
      <div className="flex flex-col justify-between gap-4 md:flex-row md:items-start">
        <div>
          <p className="text-xs font-black tracking-[0.14em] text-slate-400">DEMO HASH CHAIN</p>
          <h2 className="mt-1 text-xl font-black text-slate-800">랜덤 상품 데이터 위변조</h2>
          <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-500">
            상품의 데이터를 변경하여 저장된 노드 데이터를 수정하고 체인 과정을 확인해보세요.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={onCreate}
            disabled={loading || processing}
            className="inline-flex items-center gap-2 rounded-xl bg-slate-800 px-4 py-3 text-sm font-bold text-white hover:bg-slate-700 disabled:bg-slate-400"
          >
            <RefreshCw size={16} className={loading ? 'animate-spin' : ''} />
            {loading ? '체인 생성 중...' : '시연용 체인 생성'}
          </button>
          {chain.length > 0 && (
            <button
              type="button"
              onClick={onRestore}
              disabled={processing || loading}
              className="inline-flex items-center gap-2 rounded-xl border border-slate-300 px-4 py-3 text-sm font-bold text-slate-700 hover:bg-slate-50"
            >
              <RefreshCw size={16} /> 원상 복구
            </button>
          )}
        </div>
      </div>

      {chain.length === 0 ? (
        <div className="mt-6 rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-8 text-center text-sm font-bold text-slate-500">
          버튼을 누르면 현재 상품 중 하나를 기준으로 5개의 거래 노드를 생성합니다.
        </div>
      ) : (
        <>
          <div className="mt-5 flex flex-wrap items-center gap-2 text-sm font-bold">
            <span className={`rounded-full px-3 py-1.5 ${processing ? 'bg-sky-100 text-sky-700' : invalidCount === 0 ? 'bg-sky-100 text-sky-700' : 'bg-red-100 text-red-700'}`}>
              {processing ? `노드 검증 진행 ${progress}/${chain.length}` : invalidCount === 0 ? '전체 연결 정상' : `오류 노드 ${invalidCount}개`}
            </span>
            <span className="text-slate-500">상품 #{product?.productId} · {product?.title}</span>
            {tampered && <span className="text-red-600">중간 값 변경으로 이후 연결이 끊어졌습니다.</span>}
          </div>

          <div className="mt-4 rounded-2xl border border-slate-200 bg-slate-50 p-4">
            <label htmlFor="demo-tamper-price" className="block text-xs font-black text-slate-600">변조할 거래 가격</label>
            <div className="mt-2 flex flex-col gap-2 sm:flex-row sm:items-center">
              <input
                id="demo-tamper-price"
                type="text"
                inputMode="decimal"
                value={tamperPrice}
                onChange={(event) => onTamperPriceChange(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === 'Enter' && !tampered && !processing) {
                    event.preventDefault();
                    onTamper(middleIndex);
                  }
                }}
                disabled={tampered || processing}
                className={`w-full rounded-xl border bg-white px-4 py-3 font-mono text-lg font-black text-slate-800 outline-none focus:ring-2 disabled:bg-slate-100 sm:max-w-xs ${priceOverLimit ? 'border-red-400 focus:border-red-500 focus:ring-red-100' : 'border-slate-300 focus:border-red-400 focus:ring-red-100'}`}
                placeholder="예: 35000"
              />
              <span className="text-xs text-slate-500">현재 가격과 다른 값을 입력한 뒤 중간 가격 노드에서 변조를 실행합니다.</span>
            </div>
            {priceOverLimit && <p className="mt-2 text-sm font-bold text-red-600">변조 가격은 5,000,000원 이하로 입력해주세요.</p>}
          </div>

          <div className="mt-5 flex gap-3 overflow-x-auto pb-3">
            {chain.map((node, index) => (
              <React.Fragment key={node.sequence}>
                {(() => {
                  const pending = processing && index >= progress;
                  const normal = !pending && node.valid;
                  return (
                    <article className={`min-w-[235px] rounded-2xl border p-5 ${pending ? 'border-slate-200 bg-slate-50' : normal ? 'border-sky-200 bg-sky-50' : 'border-red-300 bg-red-50'}`}>
                      <div className="flex items-center justify-between gap-2">
                        <p className={`text-xs font-black tracking-wide ${pending ? 'text-slate-500' : normal ? 'text-sky-700' : 'text-red-700'}`}>NODE #{node.sequence}</p>
                        {pending ? <Clock size={17} className="text-slate-400" /> : normal ? <CheckCircle size={17} className="text-sky-600" /> : <AlertTriangle size={17} className="text-red-600" />}
                      </div>
                      <h3 className="mt-3 font-black text-slate-800">{node.title}</h3>
                      <p className={`mt-1 text-xs font-bold ${pending ? 'text-slate-400' : normal ? 'text-sky-700' : 'text-red-600'}`}>
                        {pending ? '검증 대기' : normal ? '정상 연결' : '위변조 감지'}
                      </p>
                      <p className="mt-1 text-sm text-slate-600">{node.detail}</p>
                      <dl className="mt-4 space-y-2 text-xs">
                        <div><dt className="font-bold text-slate-500">이전 노드 지문</dt><dd className="mt-1 break-all font-mono text-slate-700">{shortHash(node.previousHash)}</dd></div>
                        <div><dt className="font-bold text-slate-500">현재 노드 지문</dt><dd className={`mt-1 break-all font-mono font-bold ${pending ? 'text-slate-500' : normal ? 'text-sky-700' : 'text-red-700'}`}>{shortHash(node.storedHash)}</dd></div>
                      </dl>
                      {index === middleIndex && (
                        <button
                          type="button"
                          onClick={() => onTamper(index)}
                          disabled={tampered || processing}
                          className="mt-4 w-full rounded-lg border border-red-200 bg-white px-3 py-2 text-xs font-black text-red-600 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50"
                        >
                          이 가격 노드 변조하기
                        </button>
                      )}
                    </article>
                  );
                })()}
                {index < chain.length - 1 && <div className="flex min-w-[28px] items-center justify-center text-2xl font-black text-slate-300">→</div>}
              </React.Fragment>
            ))}
          </div>
          <p className="text-xs leading-5 text-slate-400">빨간 노드는 데이터 자체의 해시가 맞지 않거나, 앞 노드와 연결된 해시가 달라진 상태입니다. 이 영역은 실제 DB·블록체인을 변경하지 않는 시연용 데이터입니다.</p>
          <details open className="mt-6 rounded-2xl border border-slate-200 bg-slate-900 p-5 text-xs text-slate-300">
            <summary className="cursor-pointer font-bold text-white">시연용 체인 추가 해시 정보</summary>
            <div className="mt-4 grid gap-3 md:grid-cols-3">
              <div className="rounded-xl bg-slate-800 p-3">
                <p className="text-slate-400">대상 상품</p>
                <p className="mt-1 font-bold text-white">#{product?.productId} {product?.title}</p>
              </div>
              <div className="rounded-xl bg-slate-800 p-3">
                <p className="text-slate-400">Genesis 해시</p>
                <p className="mt-1 break-all font-mono text-[11px] text-sky-200">{DEMO_GENESIS_HASH}</p>
              </div>
              <div className="rounded-xl bg-slate-800 p-3">
                <p className="text-slate-400">마지막 노드 해시</p>
                <p className="mt-1 break-all font-mono text-[11px] text-sky-200">{chain[chain.length - 1]?.storedHash || '-'}</p>
              </div>
            </div>
            <div className="mt-4 overflow-x-auto rounded-xl border border-slate-700">
              <table className="w-full min-w-[900px] text-left font-mono text-[11px]">
                <thead className="bg-slate-800 text-slate-300">
                  <tr>
                    <th className="px-3 py-2">노드</th>
                    <th className="px-3 py-2">이전 해시</th>
                    <th className="px-3 py-2">저장된 해시</th>
                    <th className="px-3 py-2">재계산 해시</th>
                    <th className="px-3 py-2">검증 결과</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-700">
                  {chain.map((node) => (
                    <tr key={`hash-detail-${node.sequence}`} className={node.valid ? 'text-slate-300' : 'text-red-300'}>
                      <td className="px-3 py-2 font-bold">#{node.sequence}</td>
                      <td className="max-w-[230px] break-all px-3 py-2">{node.previousHash}</td>
                      <td className="max-w-[230px] break-all px-3 py-2">{node.storedHash}</td>
                      <td className="max-w-[230px] break-all px-3 py-2">{node.expectedHash}</td>
                      <td className="px-3 py-2 font-bold">
                        {!node.hashValid ? '데이터 해시 불일치' : !node.linkValid ? '이전 연결 불일치' : '정상'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </details>
        </>
      )}
    </section>

  );
};

const AdminSimulatorPage = () => {
  const navigate = useNavigate();
  const [mode, setMode] = useState('RANDOM');
  const [products, setProducts] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [preview, setPreview] = useState(null);
  const [newPrice, setNewPrice] = useState('');
  const [simulation, setSimulation] = useState(null);
  const [activeSimulationByProduct, setActiveSimulationByProduct] = useState({});
  const [productPage, setProductPage] = useState(1);
  const [loadingProducts, setLoadingProducts] = useState(false);
  const [running, setRunning] = useState(false);
  const [error, setError] = useState('');
  const [logs, setLogs] = useState([]);
  const [migration, setMigration] = useState(null);
  const [migrating, setMigrating] = useState(false);
  const [migrationOpen, setMigrationOpen] = useState(false);
  const [demoChain, setDemoChain] = useState([]);
  const [demoProduct, setDemoProduct] = useState(null);
  const [demoTamperPrice, setDemoTamperPrice] = useState('');
  const [demoLoading, setDemoLoading] = useState(false);
  const [demoProcessing, setDemoProcessing] = useState(false);
  const [demoProgress, setDemoProgress] = useState(5);
  const priceInputRef = useRef(null);
  const simulationResultRef = useRef(null);

  const addLog = (message) => setLogs((previous) => [...previous, message]);

  const loadProducts = async () => {
    setLoadingProducts(true);
    setError('');
    try {
      const response = await fetch(`${API}/simulator/products`, { credentials: 'include' });
      if (!response.ok) throw new Error(await getErrorMessage(response));
      const data = await response.json();
      setProducts(data);
      return data;
    } catch (e) {
      setError(e.message);
    } finally {
      setLoadingProducts(false);
    }
  };

  useEffect(() => {
    const savedId = sessionStorage.getItem(SIMULATION_ID_KEY);
    if (savedId) {
      fetch(`${API}/simulations/${savedId}`, { credentials: 'include' })
        .then(async (response) => (response.ok ? response.json() : null))
        .then((data) => {
          if (!data) return;
          setSimulation(data);
          if (data.status !== 'VERIFIED') {
            setActiveSimulationByProduct((previous) => ({ ...previous, [data.productId]: data }));
          }
        })
        .catch(() => sessionStorage.removeItem(SIMULATION_ID_KEY));
    }
  }, []);

  const changeMode = (nextMode) => {
    setMode(nextMode);
    setProductPage(1);
    if (nextMode === 'RANDOM') {
      setSelectedId(null);
      setPreview(null);
      setNewPrice('');
      return;
    }
    if (products.length === 0) loadProducts();
  };

  const totalProductPages = Math.max(1, Math.ceil(products.length / PRODUCTS_PER_PAGE));
  const pagedProducts = products.slice(
    (productPage - 1) * PRODUCTS_PER_PAGE,
    productPage * PRODUCTS_PER_PAGE,
  );

  useEffect(() => {
    if (productPage > totalProductPages) setProductPage(totalProductPages);
  }, [productPage, totalProductPages]);

  const selectProduct = async (product) => {
    setSelectedId(product.productId);
    setError('');
    setPreview(null);
    setNewPrice('');
    try {
      const response = await fetch(`${API}/simulator/products/${product.productId}/preview`, {
        credentials: 'include',
      });
      if (!response.ok) throw new Error(await getErrorMessage(response));
      setPreview(await response.json());
    } catch (e) {
      setError(e.message);
    }
  };

  useEffect(() => {
    if (mode !== 'TARGETED' || !preview) return;
    window.requestAnimationFrame(() => {
      priceInputRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    });
  }, [mode, preview]);

  useEffect(() => {
    if (!simulation) return;
    window.requestAnimationFrame(() => {
      simulationResultRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
  }, [simulation]);

  const verifySimulation = async (simulationId) => {
    const response = await fetch(`${API}/simulations/${simulationId}/verify`, {
      method: 'POST',
      credentials: 'include',
    });
    if (!response.ok) throw new Error(await getErrorMessage(response));
    return response.json();
  };

  const runSimulation = async () => {
    if (mode === 'TARGETED' && (!selectedId || !newPrice)) {
      setError('상품을 선택하고 변경할 가격을 입력해주세요.');
      return;
    }
    if (mode === 'TARGETED') {
      const targetedPrice = Number(String(newPrice).replace(/,/g, '').trim());
      if (!Number.isFinite(targetedPrice) || targetedPrice <= 0 || targetedPrice > MAX_TAMPER_PRICE) {
        setError('변경할 금액은 0보다 크고 5,000,000원 이하의 숫자로 입력해주세요.');
        return;
      }
    }
    if (!window.confirm(mode === 'RANDOM'
      ? '검증 가능한 상품 중 하나를 무작위로 선택해 999,999원으로 변조하시겠습니까?'
      : '선택한 상품의 가격을 DB에서 직접 변경해 위변조를 시연하시겠습니까?')) return;

    setRunning(true);
    setError('');
    setLogs([]);
    try {
      addLog(mode === 'RANDOM'
        ? '▶ 검증 가능한 상품 중 무작위 대상을 선택하고 있습니다.'
        : `▶ 상품 #${selectedId}의 실행 가능 여부를 확인하고 있습니다.`);
      const response = await fetch(`${API}/simulations`, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          mode,
          productId: mode === 'TARGETED' ? Number(selectedId) : null,
          newPrice: mode === 'TARGETED' ? Number(newPrice) : null,
          reason: mode === 'RANDOM' ? '관리자 빠른 랜덤 시연' : '관리자 직접 선택 시연',
          idempotencyKey: `${mode}-${Date.now()}-${Math.random().toString(36).slice(2)}`,
        }),
      });
      if (!response.ok) throw new Error(await getErrorMessage(response));
      let data = await response.json();
      setSimulation(data);
      setActiveSimulationByProduct((previous) => ({ ...previous, [data.productId]: data }));
      sessionStorage.setItem(SIMULATION_ID_KEY, data.simulationId);
      addLog(`✅ 상품 #${data.productId} 「${data.productTitle}」의 DB 값이 변경되었습니다.`);
      data = await verifySimulation(data.simulationId);
      setSimulation(data);
      addLog(data.status === 'FORGED_DETECTED'
        ? '🚨 DB 지문과 블록체인 원본 지문이 달라 변조를 감지했습니다.'
        : `⚠️ ${statusText[data.status] || data.message}`);
      await loadProducts();
    } catch (e) {
      setError(e.message);
      addLog(`❌ ${e.message}`);
    } finally {
      setRunning(false);
    }
  };

  const restoreSimulationById = async (simulationId) => {
    if (!simulationId) return;
    if (!window.confirm('변조 직전 저장한 실제 DB 값으로 복구하고 블록체인과 다시 비교하시겠습니까?')) return;
    setRunning(true);
    setError('');
    try {
      addLog('▶ 저장된 스냅샷으로 상품 가격을 복구하고 있습니다.');
      const response = await fetch(`${API}/simulations/${simulationId}/restore`, {
        method: 'POST',
        credentials: 'include',
      });
      if (!response.ok) throw new Error(await getErrorMessage(response));
      const data = await response.json();
      setSimulation(data);
      addLog(data.status === 'VERIFIED'
        ? '✅ 복구 후 DB 지문과 블록체인 원본 지문이 다시 일치합니다.'
        : `⚠️ ${statusText[data.status] || data.message}`);
      if (data.status === 'VERIFIED') {
        sessionStorage.removeItem(SIMULATION_ID_KEY);
        setPreview(null);
        setNewPrice('');
        setActiveSimulationByProduct((previous) => {
          const next = { ...previous };
          delete next[data.productId];
          return next;
        });
      }
      await loadProducts();
    } catch (e) {
      setError(e.message);
      addLog(`❌ ${e.message}`);
    } finally {
      setRunning(false);
    }
  };

  const restoreSimulation = () => restoreSimulationById(simulation?.simulationId);

  const restoreProduct = (product) => {
    const knownSimulation = product?.simulationId
      ? product
      : activeSimulationByProduct[product?.productId];
    if (!knownSimulation?.simulationId) return;
    setSelectedId(product.productId);
    restoreSimulationById(knownSimulation.simulationId);
  };

  const generateDemoChain = async () => {
    setDemoLoading(true);
    setError('');
    try {
      const availableProducts = products.length > 0 ? products : await loadProducts();
      const candidates = availableProducts?.filter((product) => product.canSimulate || product.blockchainStatus === 'CONFIRMED') || [];
      const sourceProduct = candidates.length > 0
        ? candidates[Math.floor(Math.random() * candidates.length)]
        : availableProducts?.[0];
      if (!sourceProduct) {
        throw new Error('시연용 체인을 만들 상품이 없습니다.');
      }
      const chain = await createDemoChain(sourceProduct);
      setDemoProduct(sourceProduct);
      setDemoTamperPrice(String(Math.min(Number(sourceProduct.price || 0) + 30000, MAX_TAMPER_PRICE)));
      setDemoChain(chain);
      setDemoProgress(chain.length);
      setDemoProcessing(false);
      addLog(`✅ 상품 #${sourceProduct.productId} 기준 시연용 거래 노드 5개를 생성했습니다.`);
    } catch (e) {
      setError(e.message);
    } finally {
      setDemoLoading(false);
    }
  };

  const tamperDemoNode = async (index) => {
    const target = demoChain[index];
    if (!target) return;
    const numericPrice = Number(String(demoTamperPrice).replace(/,/g, '').trim());
    const basePrice = Number(demoProduct?.price || 0);
    if (!Number.isFinite(numericPrice) || numericPrice <= 0 || numericPrice > MAX_TAMPER_PRICE) {
      setError('변조 가격은 0보다 크고 5,000,000원 이하의 숫자로 입력해주세요.');
      return;
    }
    if (numericPrice === basePrice) {
      setError('변조 가격은 현재 가격과 다른 값이어야 합니다.');
      return;
    }
    const tamperedPayload = JSON.stringify({
      ...JSON.parse(target.payload),
      detail: `거래 가격 ${numericPrice.toLocaleString()}원 (사용자 입력 변조)`,
      price: numericPrice,
      tampered: true,
    });
    const tamperedChain = demoChain.map((node, nodeIndex) => (nodeIndex === index
      ? { ...node, payload: tamperedPayload, detail: `${node.detail} → 변조됨` }
      : node));
    const validatedChain = await validateDemoChain(tamperedChain);
    setDemoProcessing(true);
    setDemoProgress(0);
    for (let step = 1; step <= validatedChain.length; step += 1) {
      await new Promise((resolve) => setTimeout(resolve, 1000));
      setDemoProgress(step);
      setDemoChain(validatedChain);
    }
    setDemoProcessing(false);
    addLog('🚨 중간 거래 노드의 값을 변경했습니다. 이후 노드 연결을 재검증합니다.');
  };

  const restoreDemoChain = async () => {
    if (!demoProduct) return;
    const chain = await createDemoChain(demoProduct);
    setDemoTamperPrice(String(Math.min(Number(demoProduct.price || 0) + 30000, MAX_TAMPER_PRICE)));
    setDemoChain(chain);
    setDemoProgress(chain.length);
    setDemoProcessing(false);
    addLog('✅ 시연용 거래 노드를 원본 값으로 되돌렸습니다.');
  };

  const runMigration = async () => {
    if (!window.confirm('온체인 기록이 없는 레거시 상품을 동기화하시겠습니까?')) return;
    setMigrating(true);
    setError('');
    try {
      const response = await fetch(`${API}/migrate-legacy`, {
        method: 'POST',
        credentials: 'include',
      });
      if (!response.ok) throw new Error(await getErrorMessage(response));
      let data = await response.json();
      setMigration(data);
      while (data.status === 'QUEUED' || data.status === 'RUNNING') {
        await new Promise((resolve) => setTimeout(resolve, 1500));
        const statusResponse = await fetch(`${API}/migrate-legacy/${data.jobId}`, { credentials: 'include' });
        if (!statusResponse.ok) throw new Error(await getErrorMessage(statusResponse));
        data = await statusResponse.json();
        setMigration(data);
      }
    } catch (e) {
      setError(e.message);
    } finally {
      setMigrating(false);
    }
  };

  const resultStatus = simulation?.status;
  const canRestore = simulation && ['FORGED_DETECTED', 'VERIFICATION_UNAVAILABLE', 'HASH_MISMATCH', 'RECOVERY_REQUIRED', 'RESTORED'].includes(resultStatus);

  return (
    <div className="min-h-screen bg-slate-50">
      <Header />
      <section className="bg-slate-900 px-6 py-12 text-white shadow-md">
        <div className="mx-auto flex max-w-[1500px] flex-col gap-3">
          <span className="w-max rounded-full border border-red-400/30 bg-red-500/20 px-3 py-1 text-xs font-bold tracking-widest text-red-300">
            BLOCKCHAIN INTEGRITY DEMO
          </span>
          <h1 className="text-4xl font-extrabold">보안 검증 시뮬레이터</h1>
          <p className="max-w-3xl text-base leading-relaxed text-slate-300">
            데이터베이스의 현재 상품 정보와 블록체인에 등록된 원본 지문을 비교해 변조 여부를 확인합니다.
          </p>
        </div>
      </section>

      <main className="mx-auto flex w-full max-w-[1500px] flex-col gap-6 p-6 md:p-8">
        <button onClick={() => navigate('/admin/security')} className="w-max text-sm text-blue-600 hover:underline">
          ← 보안 로그로 돌아가기
        </button>

        <div className="flex rounded-2xl border border-slate-200 bg-white p-1 shadow-sm">
          <button
            onClick={() => changeMode('RANDOM')}
            className={`flex-1 rounded-xl px-4 py-3 text-sm font-bold transition ${mode === 'RANDOM' ? 'bg-red-600 text-white' : 'text-slate-600 hover:bg-slate-100'}`}
          >
            빠른 랜덤 시연
          </button>
          <button
            onClick={() => changeMode('TARGETED')}
            className={`flex-1 rounded-xl px-4 py-3 text-sm font-bold transition ${mode === 'TARGETED' ? 'bg-blue-600 text-white' : 'text-slate-600 hover:bg-slate-100'}`}
          >
            직접 선택 시연
          </button>
        </div>
        {mode === 'RANDOM' && (
          <DemoHashChain
            chain={demoChain}
            product={demoProduct}
            loading={demoLoading}
            processing={demoProcessing}
            progress={demoProgress}
            tamperPrice={demoTamperPrice}
            onTamperPriceChange={(value) => {
              setDemoTamperPrice(value);
              setError('');
            }}
            onCreate={generateDemoChain}
            onTamper={tamperDemoNode}
            onRestore={restoreDemoChain}
          />
        )}

        {mode === 'TARGETED' && (
          <section className="scroll-mt-6 rounded-3xl border border-slate-200 bg-white p-6 shadow-sm md:p-8">
            {preview && !preview.canSimulate && <p className="mt-4 text-sm font-bold text-amber-700">이 상품은 현재 블록체인 기준 지문을 확인할 수 없어 실행할 수 없습니다.</p>}

            <div>
              {loadingProducts ? <p className="py-10 text-center text-slate-500">상품 목록을 불러오는 중입니다.</p> : (
                <div className="overflow-x-auto rounded-2xl border border-slate-200 bg-white">
                  <table className="w-full min-w-[860px] text-sm">
                    <thead className="bg-slate-50 text-left text-xs font-bold uppercase tracking-wide text-slate-500">
                      <tr>
                        <th className="w-12 px-4 py-3"><span className="sr-only">선택</span></th>
                        <th className="px-4 py-3">번호</th>
                        <th className="px-4 py-3">상품명</th>
                        <th className="px-4 py-3">분류</th>
                        <th className="px-4 py-3">상태</th>
                        <th className="px-4 py-3 text-right">현재 가격</th>
                        <th className="px-4 py-3 text-right">금액 변경하기</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {pagedProducts.map((product) => {
                        const selected = selectedId === product.productId;
                        const knownSimulation = product.simulationId
                          ? product
                          : activeSimulationByProduct[product.productId];
                        const tampered = Boolean(product.tampered || knownSimulation);
                        const originalPrice = product.originalPrice ?? knownSimulation?.originalPrice;
                        const enabled = !tampered
                          && product.blockchainStatus === 'CONFIRMED'
                          && Boolean(product.canSimulate);
                        return (
                          <React.Fragment key={product.productId}>
                            <tr
                              onClick={() => enabled && selectProduct(product)}
                              onKeyDown={(event) => {
                                if (enabled && (event.key === 'Enter' || event.key === ' ')) {
                                  event.preventDefault();
                                  selectProduct(product);
                                }
                              }}
                              tabIndex={enabled ? 0 : -1}
                              aria-disabled={!enabled}
                              className={`transition ${enabled ? 'cursor-pointer hover:bg-slate-50' : 'cursor-not-allowed opacity-60'} ${selected ? 'bg-slate-100' : 'bg-white'}`}
                            >
                              <td className="px-4 py-4">
                                <input type="checkbox" checked={selected} readOnly disabled={!enabled} className="h-4 w-4 rounded border-slate-300 accent-slate-600" aria-label={`${product.title} 선택`} />
                              </td>
                              <td className="px-4 py-4 font-mono font-bold text-slate-600">#{product.productId}</td>
                              <td className="max-w-[240px] px-4 py-4">
                                <p className="truncate font-bold text-slate-800">{product.title}</p>
                                <p className="mt-1 text-xs text-slate-400">{product.seller || '판매자 정보 없음'}</p>
                                {tampered && <p className="mt-2 font-bold text-red-600">⚠ 해시 위변조 감지 · 가격 {Number(originalPrice || 0).toLocaleString()}원 → {Number(product.price || 0).toLocaleString()}원</p>}
                              </td>
                              <td className="px-4 py-4 text-slate-600">{product.category || '기타'}</td>
                              <td className="px-4 py-4">
                                <span className={`inline-flex items-center gap-1 rounded-full px-3 py-1 text-xs font-bold ${tampered ? 'bg-red-100 text-red-700' : enabled ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500'}`}>
                                  {tampered ? <AlertTriangle size={14} /> : enabled ? <CheckCircle size={14} /> : <Clock size={14} />}
                                  {tampered ? '변조 감지' : enabled ? '실행 가능' : '대기'}
                                </span>
                              </td>
                              <td className="px-4 py-4 text-right font-mono font-bold text-slate-700">{Number(product.price || 0).toLocaleString()}원</td>
                              <td className="px-4 py-4 text-right">
                                <button
                                  type="button"
                                  onClick={(event) => {
                                    event.stopPropagation();
                                    if (tampered) {
                                      restoreProduct(product);
                                    } else if (selected && preview?.productId === product.productId) {
                                      runSimulation();
                                    } else {
                                      selectProduct(product);
                                    }
                                  }}
                                  disabled={!enabled && !tampered}
                                  className={`rounded-lg px-3 py-2 text-xs font-bold text-white transition disabled:cursor-not-allowed disabled:bg-slate-300 ${tampered ? 'bg-red-600 hover:bg-red-700' : 'bg-slate-800 hover:bg-slate-700'}`}
                                >
                                  {tampered ? '정상가로 되돌리기' : '금액 변경하기'}
                                </button>
                              </td>
                            </tr>
                            {selected && preview?.productId === product.productId && (
                              <tr ref={priceInputRef} className="bg-slate-50">
                                <td colSpan={7} className="px-4 py-4">
                                  <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
                                    <table className="w-full min-w-[620px] text-sm">
                                      <thead className="bg-slate-50 text-left text-xs font-bold text-slate-500">
                                        <tr>
                                          <th className="px-4 py-3">기존 금액</th>
                                          <th className="px-4 py-3">변경할 금액</th>
                                          <th className="px-4 py-3">수정 예정 금액</th>
                                        </tr>
                                      </thead>
                                      <tbody>
                                        <tr>
                                          <td className="px-4 py-4 font-mono text-lg font-black text-slate-700">{Number(preview.price || 0).toLocaleString()}원</td>
                                          <td className="px-4 py-4">
                                            <label className="sr-only" htmlFor={`target-price-${product.productId}`}>변경할 금액</label>
                                            <input
                                              id={`target-price-${product.productId}`}
                                              type="text"
                                              inputMode="decimal"
                                              pattern="[0-9]*\.?[0-9]*"
                                              value={newPrice}
                                              onChange={(event) => {
                                                setNewPrice(event.target.value);
                                                setError('');
                                              }}
                                              disabled={!preview.canSimulate}
                                              placeholder="금액 입력"
                                              className={`w-full rounded-xl border bg-white p-3 font-bold text-slate-800 outline-slate-500 disabled:cursor-not-allowed disabled:bg-slate-100 ${Number(String(newPrice).replace(/,/g, '').trim()) > MAX_TAMPER_PRICE ? 'border-red-400' : 'border-slate-300'}`}
                                            />
                                            {Number(String(newPrice).replace(/,/g, '').trim()) > MAX_TAMPER_PRICE && (
                                              <p className="mt-2 text-xs font-bold text-red-600">변경할 금액은 5,000,000원 이하로 입력해주세요.</p>
                                            )}
                                          </td>
                                          <td className="px-4 py-4 font-bold text-slate-600">
                                            {newPrice ? `${Number(newPrice).toLocaleString()}원` : '-'}
                                          </td>
                                        </tr>
                                      </tbody>
                                    </table>
                                  </div>
                                </td>
                              </tr>
                            )}
                          </React.Fragment>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              )}
              {!loadingProducts && products.length === 0 && <p className="py-10 text-center text-slate-500">상품이 없습니다.</p>}
              {!loadingProducts && products.length > 0 && (
                <div className="mt-6 flex items-center justify-center gap-3">
                  <button
                    type="button"
                    onClick={() => setProductPage((page) => Math.max(1, page - 1))}
                    disabled={productPage === 1}
                    className="rounded-lg border border-slate-300 px-3 py-2 text-sm font-bold text-slate-600 disabled:cursor-not-allowed disabled:opacity-40"
                  >
                    이전
                  </button>
                  <span className="text-sm font-bold text-slate-600">{productPage} / {totalProductPages}</span>
                  <button
                    type="button"
                    onClick={() => setProductPage((page) => Math.min(totalProductPages, page + 1))}
                    disabled={productPage === totalProductPages}
                    className="rounded-lg border border-slate-300 px-3 py-2 text-sm font-bold text-slate-600 disabled:cursor-not-allowed disabled:opacity-40"
                  >
                    다음
                  </button>
                </div>
              )}
            </div>
          </section>
        )}

        {simulation && (
          <section ref={simulationResultRef} className="scroll-mt-6 rounded-3xl border border-slate-200 bg-white p-6 shadow-sm md:p-8">
            <div className="flex flex-col justify-between gap-3 md:flex-row md:items-center">
              <div><p className="text-sm font-bold text-slate-500">선택 상품</p><h2 className="text-2xl font-black text-slate-800">#{simulation.productId} {simulation.productTitle}</h2></div>
              <span className={`w-max rounded-full px-4 py-2 text-sm font-black ${resultStatus === 'VERIFIED' ? 'bg-emerald-100 text-emerald-700' : resultStatus === 'FORGED_DETECTED' ? 'bg-red-100 text-red-700' : 'bg-amber-100 text-amber-700'}`}>
                {statusText[resultStatus] || resultStatus}
              </span>
            </div>

            {/* 기존 영수증형 상품 변경 비교 UI는 노드 중심 시연을 위해 보류합니다. */}

            <div className={`mt-5 rounded-2xl p-5 text-center text-lg font-black ${resultStatus === 'VERIFIED' ? 'bg-emerald-100 text-emerald-700' : resultStatus === 'FORGED_DETECTED' ? 'bg-red-100 text-red-700' : 'bg-amber-100 text-amber-700'}`}>
              {resultStatus === 'FORGED_DETECTED' ? '🚨 DB 값만 변경되어 원본과 달라졌습니다.' : resultStatus === 'VERIFIED' ? '✅ 복구 후 두 데이터의 지문이 다시 일치합니다.' : (statusText[resultStatus] || simulation.message)}
            </div>

            <div className="mt-6 flex flex-wrap items-center justify-center gap-2 text-sm font-bold text-slate-500">
              <span className="rounded-full bg-slate-100 px-3 py-2">상품 등록</span><span>→</span><span className="rounded-full bg-slate-100 px-3 py-2">정상 확인</span><span>→</span><span className={`rounded-full px-3 py-2 ${resultStatus === 'FORGED_DETECTED' ? 'bg-red-100 text-red-700' : 'bg-slate-100'}`}>DB 변조·탐지</span><span>→</span><span className={`rounded-full px-3 py-2 ${resultStatus === 'VERIFIED' ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100'}`}>복구·재검증</span>
            </div>

            {canRestore && resultStatus !== 'VERIFIED' && (
              <div className="mt-7 flex justify-center">
                <button
                  onClick={restoreSimulation}
                  disabled={running}
                  className="inline-flex items-center gap-2 rounded-xl bg-blue-600 px-7 py-3 font-bold text-white hover:bg-blue-700 disabled:bg-slate-400"
                >
                  <RefreshCw size={19} className={running ? 'animate-spin' : ''} />
                  원래 값으로 복구
                </button>
              </div>
            )}
            {logs.length > 0 && (
              <section className="mt-4 w-full rounded-3xl bg-slate-900 p-6 text-sm text-slate-200 shadow-xl">
                <h2 className="mb-4 font-bold text-white">시연 기록</h2>
                {logs.map((log, index) => (
                  <p key={`${log}-${index}`} className="border-l-2 border-blue-400 py-1 pl-3">
                    {log}
                  </p>
                ))}
              </section>
            )}
            <details open className="mt-6 rounded-xl bg-slate-900 p-4 text-xs text-slate-300">
              <summary className="cursor-pointer font-bold text-slate-200">기술 정보 보기</summary>
              <div className="mt-3 grid gap-2 break-all md:grid-cols-2">
                <p>실행 ID: {simulation.simulationId}</p>
                <p>모드: {simulation.mode}</p>
                <p>변조 전 DB 지문: {simulation.originalDbHash}</p>
                <p>블록체인 원본 지문: {simulation.activeOnChainHash}</p>
                <p>변조 후 DB 지문: {simulation.tamperedDbHash || '-'}</p>
                <p>현재 DB 지문: {simulation.currentDbHash || '-'}</p>
              </div>
            </details>

          </section>
        )}


        {error && <div className="flex items-start gap-3 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm font-bold text-red-700"><ServerCrash size={20} className="shrink-0" />{error}</div>}

        <section className="rounded-3xl border border-slate-200 bg-white shadow-sm md:p-2">
          <button
            type="button"
            onClick={() => setMigrationOpen((open) => !open)}
            className="flex w-full items-center justify-between gap-4 p-6 text-left md:p-6"
            aria-expanded={migrationOpen}
          >
            <span>
              <span className="flex items-center gap-2 text-lg font-bold text-slate-800"><Database size={20} className="text-blue-600" /> 레거시 데이터 동기화</span>
              <span className="mt-1 block text-sm text-slate-500">필요할 때만 과거 상품의 온체인 원본을 동기화합니다.</span>
            </span>
            <ChevronDown size={20} className={`shrink-0 text-slate-500 transition-transform ${migrationOpen ? 'rotate-180' : ''}`} />
          </button>
          {migrationOpen && (
            <div className="border-t border-slate-100 px-6 pb-6 pt-5">
              <div className="flex flex-col justify-between gap-4 md:flex-row md:items-center">
                <p className="text-sm text-slate-500">동기화를 시작하면 온체인 원본이 없는 과거 상품을 별도로 처리합니다.</p>
                <button onClick={runMigration} disabled={migrating || running} className="inline-flex items-center justify-center gap-2 rounded-xl bg-slate-700 px-5 py-3 font-bold text-white hover:bg-slate-800 disabled:bg-slate-400">
                  {migrating ? <Clock size={18} className="animate-spin" /> : <RefreshCw size={18} />}
                  {migrating ? '동기화 중...' : '레거시 동기화 실행'}
                </button>
              </div>
              {migration && <p className="mt-4 text-sm font-bold text-slate-600">상태: {migration.status} · 처리 {migration.processedCount ?? 0}/{migration.totalCount ?? 0}</p>}
            </div>
          )}
        </section>
      </main>
    </div>
  );
};

export default AdminSimulatorPage;
