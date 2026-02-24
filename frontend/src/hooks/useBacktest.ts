import { useQuery, useMutation } from '@tanstack/react-query';
import { strategyService } from '../services/api';
import { BacktestRequest } from '../types/api';

export const useBacktest = () => {
  return useMutation({
    mutationFn: (request: BacktestRequest) => strategyService.startBacktest(request),
  });
};

export const useBacktestResult = (simulationId: number | null) => {
  return useQuery({
    queryKey: ['backtestResult', simulationId],
    queryFn: () => strategyService.getBacktestResult(simulationId!),
    enabled: !!simulationId,
    refetchInterval: (query) => {
      // Poll if result is not found yet (assuming backend creates it after completion)
      return query.state.data ? false : 3000;
    },
  });
};

export const useSnapshots = (simulationId: number | null) => {
  return useQuery({
    queryKey: ['snapshots', simulationId],
    queryFn: () => strategyService.getSnapshots(simulationId!),
    enabled: !!simulationId,
  });
};

export const useCompareStrategies = (resultIds: string) => {
  return useQuery({
    queryKey: ['compareStrategies', resultIds],
    queryFn: () => strategyService.compareStrategies(resultIds),
    enabled: !!resultIds,
  });
};
