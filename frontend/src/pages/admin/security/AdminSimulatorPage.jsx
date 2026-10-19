import React, { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AdminHeader from '../../../components/admin/AdminHeader';
import {
  AlertTriangle,
  CheckCircle,
  Clock,
  RefreshCw,
  ServerCrash,
  ShieldCheck,
} from 'lucide-react';

const API = 'http://localhost:8080/api/admin/security';
const PRODUCTS_PER_PAGE = 10;
const DEMO_GENESIS_HASH = '0'.repeat(64);
const MAX_TAMPER_PRICE = 5000000;

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
  const currentCount = Number(product?.currentCount || 0);
  const targetCount = Number(product?.targetCount || currentCount);
  const events = [
    { type: 'PRODUCT_CREATED', title: '상품 등록', detail: `상품 #${product.productId} · ${price.toLocaleString()}원` },
    { type: 'PRICE_RECORDED', title: '가격 원본 기록', detail: `기준 가격 ${price.toLocaleString()}원` },
    { type: 'PRICE_VERSION', title: '가격 버전 1', detail: `거래 가격 ${price.toLocaleString()}원` },
    { type: 'PARTICIPATION', title: '참여 기록', detail: `현재 참여 인원 ${currentCount.toLocaleString()}명` },
    { type: 'PARTICIPATION', title: '모집 목표 기록', detail: `목표 참여 인원 ${targetCount.toLocaleString()}명` },
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
    const expectedPreviousHash = previousHash;
    const expectedHash = await sha256Hex(`${expectedPreviousHash}|${node.payload}`);
    const hashValid = expectedHash === node.storedHash;
    const linkValid = node.previousHash === expectedPreviousHash;
    const valid = previousValid && hashValid && linkValid;
    validated.push({ ...node, expectedPreviousHash, expectedHash, hashValid, linkValid, valid });
    previousHash = expectedHash;
    previousValid = valid;
  }
  return validated;
};

const RECOVERY_STEPS = [
  { title: '문제 해시 감지', description: '재계산 해시가 저장 해시와 다른 노드를 식별합니다.' },
  { title: '정상 기준 해시 확인', description: '변조 전 스냅샷에 저장한 기준 해시를 확인합니다.' },
  { title: '원본 데이터 복원', description: '검증된 원본 데이터를 복원용 체인으로 구성합니다.' },
  { title: '연결 해시 재생성', description: '앞선 노드부터 순서대로 연결 해시를 다시 생성합니다.' },
  { title: '최종 무결성 검증', description: '재생성된 체인이 모두 정상인지 다시 확인합니다.' },
];

const RecoverySteps = ({ progress, completed, evidence }) => {
  if (!evidence) return null;

  return (
    <section className="mt-5 rounded-2xl border border-emerald-200 bg-emerald-50 p-5">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <p className="text-sm font-black text-emerald-800">변조 해시 복원 과정</p>
          <p className="mt-1 text-xs text-emerald-700">문제가 된 노드 #{evidence?.sequence || '-'}를 기준으로 5단계 복원 과정을 표시합니다.</p>
        </div>
        <span className="rounded-full bg-white px-3 py-1 text-xs font-black text-emerald-700">
          {completed ? '복원 및 검증 완료' : `복원 ${progress} / ${RECOVERY_STEPS.length} 단계`}
        </span>
      </div>

      <ol className="mt-4 grid gap-3 md:grid-cols-5">
        {RECOVERY_STEPS.map((step, index) => {
          const stepNumber = index + 1;
          const done = progress >= stepNumber;
          const active = !completed && progress + 1 === stepNumber;
          return (
            <li key={step.title} className={`rounded-xl border p-3 ${done ? 'border-emerald-300 bg-white' : active ? 'border-emerald-400 bg-emerald-100' : 'border-emerald-100 bg-white/60'}`}>
              <div className="flex items-center gap-2">
                <span className={`flex h-6 w-6 items-center justify-center rounded-full text-xs font-black ${done ? 'bg-emerald-600 text-white' : active ? 'bg-emerald-500 text-white' : 'bg-emerald-100 text-emerald-600'}`}>
                  {done ? <CheckCircle size={14} /> : stepNumber}
                </span>
                <p className="text-xs font-black text-slate-800">{step.title}</p>
              </div>
              <p className="mt-2 text-xs leading-5 text-slate-600">{step.description}</p>
            </li>
          );
        })}
      </ol>

      <div className="mt-4 grid gap-3 rounded-xl border border-emerald-200 bg-white p-3 text-xs md:grid-cols-2">
        <div>
          <p className="font-bold text-slate-500">문제 감지 해시</p>
          <p className="mt-1 break-all font-mono text-red-600">{evidence?.tamperedHash || '확인 중'}</p>
        </div>
        <div>
          <p className="font-bold text-slate-500">복원 기준 해시</p>
          <p className="mt-1 break-all font-mono text-emerald-700">{evidence?.trustedHash || '확인 중'}</p>
        </div>
      </div>
    </section>
  );
};

const DemoHashChain = ({ chain, product, loading, processing, progress, restoring, recoveryProgress, tamperPrice, onTamperPriceChange, onCreate, onTamper, onRestore, title = '랜덤 상품 데이터 위변조', showCreateButton = true }) => {
  const middleIndex = chain.findIndex((node) => node.type === 'PRICE_VERSION') >= 0
    ? chain.findIndex((node) => node.type === 'PRICE_VERSION')
    : Math.floor(chain.length / 2);
  const invalidCount = chain.filter((node) => !node.valid).length;
  const tampered = chain.some((node, index) => !node.hashValid && (!processing || index < progress));
  const enteredPrice = Number(String(tamperPrice).replace(/,/g, '').trim());
  const priceOverLimit = tamperPrice !== '' && Number.isFinite(enteredPrice) && enteredPrice > MAX_TAMPER_PRICE;

  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm md:p-8">
      <div className="flex flex-col justify-between gap-4 md:flex-row md:items-start">
        <div>
          <p className="text-xs font-black tracking-[0.14em] text-slate-400">DEMO HASH CHAIN</p>
          <h2 className="mt-1 text-xl font-black text-slate-800">{title}</h2>
          <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-500">
            상품의 데이터를 변경하여 저장된 노드 데이터를 수정하고 체인 과정을 확인해보세요.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {showCreateButton && (
            <button
              type="button"
              onClick={onCreate}
              disabled={loading || processing || restoring}
              className="inline-flex items-center gap-2 rounded-xl bg-slate-800 px-4 py-3 text-sm font-bold text-white hover:bg-slate-700 disabled:bg-slate-400"
            >
              <RefreshCw size={16} className={loading ? 'animate-spin' : ''} />
              {loading ? '체인 생성 중...' : '시연용 체인 생성'}
            </button>
          )}
          {chain.length > 0 && (
            <button
              type="button"
              onClick={() => onTamper(middleIndex)}
              disabled={tampered || processing || restoring || loading}
              className="inline-flex items-center gap-2 rounded-xl border border-red-200 bg-white px-4 py-3 text-sm font-bold text-red-600 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <AlertTriangle size={16} /> 노드 변조하기
            </button>
          )}
          {chain.length > 0 && (
            <button
              type="button"
              onClick={onRestore}
              disabled={processing || restoring || loading}
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
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-slate-500">상품 #{product?.productId} · {product?.title}</span>
              {tampered && <span className="text-red-600">중간 값 변경으로 이후 연결이 끊어졌습니다.</span>}
            </div>
            <span className={`ml-auto rounded-full px-3 py-1.5 ${processing ? 'bg-sky-100 text-sky-700' : invalidCount === 0 ? 'bg-sky-100 text-sky-700' : 'bg-red-100 text-red-700'}`}>
              {processing ? `노드 검증 진행 ${progress}/${chain.length}` : invalidCount === 0 ? '전체 연결 정상' : `오류 노드 ${invalidCount}개`}
            </span>
          </div>

          <div className="mt-4 rounded-2xl border border-slate-200 bg-slate-50 p-4">
            <table className="demo-price-table w-full table-fixed overflow-hidden rounded-xl border border-slate-200 bg-white text-sm">
              <thead className="bg-slate-100 text-left text-xs font-black text-slate-600">
                <tr>
                  <th className="w-1/2 border-r border-slate-200 px-4 py-3">기존 가격</th>
                  <th className="w-1/2 px-4 py-3">변경할 가격</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td className="border-r border-slate-200 px-4 py-5 font-mono text-xl font-black text-slate-700">
                    {Number(product?.price || 0).toLocaleString()}원
                  </td>
                  <td className="px-4 py-4">
                    <label htmlFor="demo-tamper-price" className="sr-only">변조할 거래 가격</label>
                    <input
                      id="demo-tamper-price"
                      type="text"
                      inputMode="decimal"
                      value={tamperPrice}
                      onChange={(event) => onTamperPriceChange(event.target.value)}
                      onKeyDown={(event) => {
                        if (event.key === 'Enter' || event.code === 'NumpadEnter') {
                          event.preventDefault();
                          onTamper(middleIndex);
                        }
                      }}
                      disabled={tampered || processing || restoring}
                      className={`w-full rounded-xl border bg-white px-4 py-3 font-mono text-lg font-black text-slate-800 outline-none focus:ring-2 disabled:bg-slate-100 ${priceOverLimit ? 'border-red-400 focus:border-red-500 focus:ring-red-100' : 'border-slate-300 focus:border-slate-500 focus:ring-slate-100'}`}
                      placeholder="예: 35000"
                    />
                  </td>
                </tr>
              </tbody>
            </table>
            <p className="mt-2 text-xs text-slate-500">현재 가격과 다른 값을 입력한 뒤 Enter를 누르면 중간 가격 노드에서 변조를 실행합니다.</p>
            {priceOverLimit && <p className="mt-2 text-sm font-bold text-red-600">변조 가격은 5,000,000원 이하로 입력해주세요.</p>}
          </div>

          <div className="mt-5 flex w-full gap-3 overflow-x-auto pb-3">
            {chain.map((node, index) => (
              <React.Fragment key={node.sequence}>
                {(() => {
                  const pending = (processing && index >= progress) || (restoring && index >= recoveryProgress);
                  const normal = !pending && node.valid;
                  const displayedPreviousHash = !pending && !node.linkValid && node.expectedPreviousHash
                    ? node.expectedPreviousHash
                    : node.previousHash;
                  return (
                    <article className={`min-w-[180px] flex-1 rounded-2xl border p-5 ${pending ? 'border-slate-200 bg-slate-50' : normal ? 'border-sky-200 bg-sky-50' : 'border-red-300 bg-red-50'}`}>
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
                        <div><dt className="font-bold text-slate-500">이전 노드 지문</dt><dd className="mt-1 break-all font-mono text-slate-700">{shortHash(displayedPreviousHash)}</dd></div>
                        {!node.hashValid ? (
                          <>
                            <div><dt className="font-bold text-slate-500">변조 전 저장 해시</dt><dd className="mt-1 break-all font-mono text-slate-700">{shortHash(node.storedHash)}</dd></div>
                            <div><dt className="font-bold text-slate-500">변조 후 재계산 해시</dt><dd className="mt-1 break-all font-mono font-bold text-red-700">{shortHash(node.expectedHash)}</dd></div>
                          </>
                        ) : (
                          <div><dt className="font-bold text-slate-500">현재 노드 지문</dt><dd className={`mt-1 break-all font-mono font-bold ${pending ? 'text-slate-500' : normal ? 'text-sky-700' : 'text-red-700'}`}>{shortHash(node.storedHash)}</dd></div>
                        )}
                      </dl>
                    </article>
                  );
                })()}
                {index < chain.length - 1 && <div className="flex min-w-[28px] items-center justify-center text-2xl font-black text-slate-300">→</div>}
              </React.Fragment>
            ))}
          </div>
          <p className="text-xs leading-5 text-slate-400">빨간 노드는 데이터 자체의 해시가 맞지 않거나, 앞 노드와 연결된 해시가 달라진 상태입니다. 이 영역은 실제 DB·블록체인을 변경하지 않는 시연용 데이터입니다.</p>
          <div className="mt-6 rounded-2xl border border-slate-200 bg-slate-900 p-5 text-xs text-slate-300">
            <p className="font-bold text-white">시연용 체인 추가 해시 정보</p>
            <div className="mt-4 grid gap-3 md:grid-cols-3">
              <div className="rounded-xl border border-sky-200 bg-sky-50 p-3">
                <p className="font-bold text-slate-500">대상 상품</p>
                <p className="mt-1 font-bold text-slate-700">
                  #{product?.productId} {product?.title}
                </p>
              </div>

              <div className="rounded-xl border border-sky-200 bg-sky-50 p-3">
                <p className="font-bold text-slate-500">Genesis 해시</p>
                <p className="font-bold mt-1 break-all text-[11px] text-slate-700">
                  {DEMO_GENESIS_HASH}
                </p>
              </div>

              <div className="rounded-xl border border-sky-200 bg-sky-50 p-3">
                <p className="font-bold text-slate-500">마지막 노드 해시</p>
                <p className="font-bold mt-1 break-all text-[11px] text-slate-700">
                  {chain[chain.length - 1]?.storedHash || '-'}
                </p>
              </div>
            </div>
            <div className="mt-4 overflow-x-auto rounded-xl border border-slate-700">
              <table className="demo-hash-table w-full min-w-[900px] text-left font-mono text-[11px]">
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
                  {chain.map((node, index) => {
                    const pending = processing && index >= progress;
                    const verificationText = pending
                      ? '검증 대기'
                      : !node.hashValid
                        ? '데이터 해시 불일치'
                        : !node.valid
                          ? '이전 연결 불일치'
                          : '정상';
                    return (
                      <tr key={`hash-detail-${node.sequence}`} className={pending ? 'text-slate-500' : node.valid ? 'text-slate-300' : 'text-red-300'}>
                        <td className="px-3 py-2 font-bold">#{node.sequence}</td>
                        <td className="max-w-[230px] break-all px-3 py-2">{node.expectedPreviousHash || node.previousHash}</td>
                        <td className="max-w-[230px] break-all px-3 py-2">{node.storedHash}</td>
                        <td className="max-w-[230px] break-all px-3 py-2">{pending ? '검증 대기 중' : node.expectedHash}</td>
                        <td className="px-3 py-2 font-bold">{verificationText}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
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
  const [productPage, setProductPage] = useState(1);
  const [loadingProducts, setLoadingProducts] = useState(false);
  const [error, setError] = useState('');
  const [demoChain, setDemoChain] = useState([]);
  const [demoBaseChain, setDemoBaseChain] = useState([]);
  const [demoProduct, setDemoProduct] = useState(null);
  const [demoTamperPrice, setDemoTamperPrice] = useState('');
  const [demoLoading, setDemoLoading] = useState(false);
  const [demoProcessing, setDemoProcessing] = useState(false);
  const [demoProgress, setDemoProgress] = useState(5);
  const [recoveryProgress, setRecoveryProgress] = useState(0);
  const [recoveryComplete, setRecoveryComplete] = useState(false);
  const [recoveryEvidence, setRecoveryEvidence] = useState(null);
  const [restoring, setRestoring] = useState(false);
  const demoChainRef = useRef(null);

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

  const changeMode = (nextMode) => {
    setMode(nextMode);
    setProductPage(1);
    setDemoChain([]);
    setDemoBaseChain([]);
    setDemoProduct(null);
    setDemoTamperPrice('');
    setDemoProcessing(false);
    setDemoProgress(5);
    setRecoveryProgress(0);
    setRecoveryComplete(false);
    setRecoveryEvidence(null);
    setRestoring(false);
    if (nextMode === 'RANDOM') {
      setSelectedId(null);
      setPreview(null);
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
    try {
      const response = await fetch(`${API}/simulator/products/${product.productId}/preview`, {
        credentials: 'include',
      });
      if (!response.ok) throw new Error(await getErrorMessage(response));
      setPreview(await response.json());
      const defaultTamperPrice = Math.min(Number(product.price || 0) + 30000, MAX_TAMPER_PRICE);
      await prepareDemoChain(product, defaultTamperPrice, true);
    } catch (e) {
      setError(e.message);
    }
  };

  const prepareDemoChain = async (sourceProduct, initialPrice, shouldScroll = false) => {
    const chain = await createDemoChain(sourceProduct);
    setDemoProduct(sourceProduct);
    setDemoTamperPrice(String(initialPrice));
    setDemoChain(chain);
    setDemoBaseChain(chain);
    setDemoProgress(chain.length);
    setDemoProcessing(false);
    setRecoveryProgress(0);
    setRecoveryComplete(false);
    setRecoveryEvidence(null);
    setRestoring(false);
    if (shouldScroll) {
      window.requestAnimationFrame(() => {
        demoChainRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      });
    }
    return chain;
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
      await prepareDemoChain(sourceProduct, Math.min(Number(sourceProduct.price || 0) + 30000, MAX_TAMPER_PRICE));
    } catch (e) {
      setError(e.message);
    } finally {
      setDemoLoading(false);
    }
  };

  const tamperDemoNode = async (index) => {
    if (demoProcessing || restoring) return;
    const sourceChain = demoBaseChain.length > 0 ? demoBaseChain : demoChain;
    const target = sourceChain[index];
    if (!target) return;
    setRecoveryProgress(0);
    setRecoveryComplete(false);
    setRecoveryEvidence(null);
    setRestoring(false);
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
    const tamperedDetail = `거래 가격 ${numericPrice.toLocaleString()}원`;
    const tamperedPayload = JSON.stringify({
      ...JSON.parse(target.payload),
      detail: tamperedDetail,
      price: numericPrice,
      tampered: true,
    });
    const tamperedChain = sourceChain.map((node, nodeIndex) => (nodeIndex === index
      ? { ...node, payload: tamperedPayload, detail: tamperedDetail, tamperedPrice: numericPrice }
      : node));
    const validatedChain = await validateDemoChain(tamperedChain);
    setDemoProcessing(true);
    setDemoProgress(0);
    for (let step = 1; step <= validatedChain.length; step += 1) {
      const nodeIndex = step - 1;
      setDemoChain((currentChain) => currentChain.map((node, currentIndex) => (
        currentIndex === nodeIndex ? validatedChain[nodeIndex] : node
      )));
      setDemoProgress(step);
      await new Promise((resolve) => setTimeout(resolve, 1000));
    }
    setDemoProcessing(false);
  };

  const restoreDemoChain = async () => {
    if (!demoProduct || demoProcessing || restoring) return;

    const damagedNode = demoChain.find((node) => !node.valid || !node.hashValid);
    setRecoveryEvidence({
      sequence: damagedNode?.sequence,
      tamperedHash: damagedNode?.expectedHash || damagedNode?.storedHash,
      trustedHash: damagedNode?.storedHash,
    });
    setRecoveryProgress(0);
    setRecoveryComplete(false);
    setRestoring(true);

    try {
      const restoredChain = demoBaseChain.length > 0
        ? demoBaseChain
        : await createDemoChain(demoProduct);
      for (let step = 1; step <= restoredChain.length; step += 1) {
        const nodeIndex = step - 1;
        setDemoChain((currentChain) => currentChain.map((node, index) => (
          index === nodeIndex ? restoredChain[index] : node
        )));
        setRecoveryProgress(step);
        await new Promise((resolve) => setTimeout(resolve, 800));
      }

      const verifiedChain = await validateDemoChain(restoredChain);
      setDemoTamperPrice(String(Number(demoProduct.price || 0)));
      setDemoChain(verifiedChain);
      setDemoProgress(verifiedChain.length);
      setRecoveryComplete(true);
    } catch (e) {
      setError(e.message || '복원 시연을 완료하지 못했습니다.');
    } finally {
      setRestoring(false);
    }
  };

  return (
    <div className="v3-page admin-v3-page min-h-screen">
      <AdminHeader />
      <div className="v3-unified-page-card">
        <header className="v3-unified-page-card__header">
          <h1 className="v3-unified-page-card__title"><ShieldCheck size={28} />보안 검증 시뮬레이터</h1>
        </header>

        <main className="v3-unified-page-card__body admin-v3-main">
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
              restoring={restoring}
              recoveryProgress={recoveryProgress}
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
            <div ref={demoChainRef} className="scroll-mt-6">
              <DemoHashChain
                chain={demoChain}
                product={demoProduct}
                loading={demoLoading}
                processing={demoProcessing}
                progress={demoProgress}
                restoring={restoring}
                recoveryProgress={recoveryProgress}
                tamperPrice={demoTamperPrice}
                onTamperPriceChange={(value) => {
                  setDemoTamperPrice(value);
                  setError('');
                }}
                onCreate={generateDemoChain}
                onTamper={tamperDemoNode}
                onRestore={restoreDemoChain}
                title="선택 상품 데이터 위변조"
                showCreateButton={false}
              />
            </div>
          )}

          <RecoverySteps
            progress={recoveryProgress}
            completed={recoveryComplete}
            evidence={recoveryEvidence}
          />

          {mode === 'TARGETED' && (
            <section className="scroll-mt-6 rounded-3xl border border-slate-200 bg-white p-6 shadow-sm md:p-8">
              {preview && !preview.canSimulate && <p className="mt-4 text-sm font-bold text-amber-700">이 상품은 현재 블록체인 기준 지문을 확인할 수 없어 실행할 수 없습니다.</p>}

              <div>
                {loadingProducts ? <p className="py-10 text-center text-slate-500">상품 목록을 불러오는 중입니다.</p> : (
                  <div className="overflow-x-auto rounded-2xl border border-slate-200 bg-white">
                    <table className="w-full min-w-[1000px] text-sm">
                      <thead className="bg-slate-50 text-left text-xs font-bold uppercase tracking-wide text-slate-500">
                        <tr>
                          <th className="w-12 px-6 py-3"><span className="sr-only">선택</span></th>
                          <th className="px-6 py-3">번호</th>
                          <th className="px-6 py-3">상품명</th>
                          <th className="px-6 py-3">분류</th>
                          <th className="px-6 py-3">상태</th>
                          <th className="px-6 py-3 text-right">현재 가격</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100">
                        {pagedProducts.map((product) => {
                          const selected = selectedId === product.productId;
                          const knownSimulation = product.simulationId ? product : null;
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
                                <td className="px-6 py-4">
                                  <input type="checkbox" checked={selected} readOnly disabled={!enabled} className="h-4 w-4 rounded border-slate-300 accent-slate-600" aria-label={`${product.title} 선택`} />
                                </td>
                                <td className="px-6 py-4 font-mono font-bold text-slate-600">#{product.productId}</td>
                                <td className="max-w-[240px] px-6 py-4">
                                  <p className="truncate font-bold text-slate-800">{product.title}</p>
                                  <p className="mt-1 text-xs text-slate-400">{product.seller || '판매자 정보 없음'}</p>
                                  {tampered && <p className="mt-2 font-bold text-red-600">⚠ 해시 위변조 감지 · 가격 {Number(originalPrice || 0).toLocaleString()}원 → {Number(product.price || 0).toLocaleString()}원</p>}
                                </td>
                                <td className="px-6 py-4 text-slate-600">{product.category || '기타'}</td>
                                <td className="px-6 py-4">
                                  <span className={`inline-flex items-center gap-1 rounded-full px-3 py-1 text-xs font-bold ${tampered ? 'bg-red-100 text-red-700' : enabled ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500'}`}>
                                    {tampered ? <AlertTriangle size={14} /> : enabled ? <CheckCircle size={14} /> : <Clock size={14} />}
                                    {tampered ? '변조 감지' : enabled ? '실행 가능' : '대기'}
                                  </span>
                                </td>
                                <td className="px-6 py-4 text-right font-mono font-bold text-slate-700">{Number(product.price || 0).toLocaleString()}원</td>
                              </tr>
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

          {error && <div className="flex items-start gap-3 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm font-bold text-red-700"><ServerCrash size={20} className="shrink-0" />{error}</div>}
        </main>
      </div>
    </div>
  );
};

export default AdminSimulatorPage;
