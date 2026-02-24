import axios from 'axios';

const api = axios.create({
  baseURL: '/api/v1', // Proxied by Vite or Nginx
  headers: {
    'Content-Type': 'application/json',
  },
});

export const strategyService = {
  startBacktest: async (request: any) => {
    const { data } = await api.post('/strategy/backtest', request);
    return data;
  },
  getBacktestResult: async (simulationId: number) => {
    const { data } = await api.get(`/strategy/backtest/${simulationId}/result`);
    return data;
  },
  getSnapshots: async (simulationId: number) => {
    const { data } = await api.get(`/strategy/backtest/${simulationId}/snapshots`);
    return data;
  },
  compareStrategies: async (resultIds: string) => {
    const { data } = await api.get(`/strategy/backtest/compare?resultIds=${resultIds}`);
    return data;
  },
  optimize: async (request: any) => {
    const { data } = await api.post('/strategy/backtest/optimize', request);
    return data;
  },
  getDashboardSummary: async () => {
    const { data } = await api.get('/strategy/backtest/summary');
    return data;
  },
};

export const corpService = {
  getUniverseCount: async () => {
    const { data } = await api.get('/corp/universe/count');
    return data;
  },
  getCorpDetail: async (stockCode: string) => {
    const formattedCode = stockCode.startsWith('A') ? stockCode : `A${stockCode}`;
    const { data } = await api.get(`/corp/internal/corp-detail/${formattedCode}`);
    return data;
  },
  searchCorps: async (query: string) => {
    const { data } = await api.get(`/corp/internal/search?query=${query}`);
    return data;
  },
  runCorpInfoBatch: async (date: string) => {
    const { data } = await api.post(`/corp/batch/corp-info?date=${date}`);
    return data;
  },
  runSectorUpdateBatch: async () => {
    const { data } = await api.post('/corp/batch/corp-detail/sector-update');
    return data;
  },
};

export const financeService = {
  getVerificationRate: async () => {
    const { data } = await api.get('/finance/quarterly/stats/verification');
    return data;
  },
  getFinanceSummary: async (stockCode: string) => {
    const formattedCode = stockCode.startsWith('A') ? stockCode : `A${stockCode}`;
    const { data } = await api.get(`/finance/internal/summary/${formattedCode}`);
    return data;
  },
  runFinanceBatch: async (date: string, reportCode?: string) => {
    const url = reportCode 
      ? `/finance/batch/corp-fin?date=${date}&reportCode=${reportCode}`
      : `/finance/batch/corp-fin?date=${date}`;
    const { data } = await api.post(url);
    return data;
  },
};

export const priceService = {
  getPriceHistory: async (stockCode: string, days = 365) => {
    const { data } = await api.get(`/stock/internal/prices/${stockCode}?days=${days}`);
    return data;
  },
  runPriceBatch: async (market: string, date: string) => {
    const { data } = await api.post(`/stock/batch/price?market=${market}&date=${date}`);
    return data;
  },
  runIndicatorBatch: async (date: string) => {
    const { data } = await api.post(`/stock/batch/indicators?date=${date}`);
    return data;
  },
};

export const systemService = {
  getServiceInfo: async () => {
    // Gateway의 RootController는 보통 / (경로)에 직접 매핑되어 있을 수 있으므로 
    // baseURL을 우회하여 호출하거나 Gateway가 라우팅하는 경로를 확인해야 함
    // 여기서는 Gateway가 /api/v1/gateway/info 등으로 매핑되어 있는지 확인이 필요하나,
    // 가장 확실한 방법은 axios 인스턴스를 직접 사용하는 대신 상대 경로를 조정하는 것
    const { data } = await api.get('/system/info'); // Gateway에 새로 정의할 경로
    return data;
  },
};

export default api;
