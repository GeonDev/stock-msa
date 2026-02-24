export interface BacktestResult {
  id: number;
  simulationId: number;
  finalValue: number;
  totalReturn: number;
  cagr: number;
  mdd: number;
  sharpeRatio: number;
  volatility: number;
  winRate: number;
  totalTrades: number;
  profitableTrades: number;
  isOptimized: boolean;
  slippageType: string;
}

export interface PortfolioSnapshot {
  id: number;
  simulationId: number;
  snapshotDate: string;
  totalValue: number;
  cashBalance: number;
  holdings: string; // JSON string of holdings
}

export interface BacktestRequest {
  strategyType: string;
  startDate: string;
  endDate: string;
  initialCapital: number;
  rebalancingPeriod: string;
  tradingFeeRate: number;
  taxRate: number;
  slippageType: string;
  fixedSlippageRate?: number;
  maxWeightPerStock?: number;
  universeFilter?: any;
  valueStrategyConfig?: any;
  multiFactorConfig?: any;
  sectorRotationConfig?: any;
  assetAllocationConfig?: any;
}

export interface BacktestResponse {
  simulationId: number;
  status: string;
  message: string;
}

export interface ServiceInfo {
  name: string;
  version: string;
  profile: string;
}
