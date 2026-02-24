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
};

export const systemService = {
  getServiceInfo: async (servicePath: string) => {
    const { data } = await api.get(`/${servicePath}/`);
    return data;
  },
};

export default api;
