import request from '../../utils/request'

export const getFinanceOverview = (): Promise<any> => request.get('/merchant/finance/overview') as any
export const getFinanceLedgers = (): Promise<any> => request.get('/merchant/finance/ledgers') as any
export const getFinanceSettlements = (): Promise<any> => request.get('/merchant/finance/settlements') as any
