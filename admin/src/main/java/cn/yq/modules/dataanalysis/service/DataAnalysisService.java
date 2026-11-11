package cn.yq.modules.dataanalysis.service;

import cn.yq.modules.dataanalysis.domain.vo.req.DataAnalysisPlanReq;
import cn.yq.modules.dataanalysis.domain.vo.req.DataAnalysisAnalyzeReq;
import cn.yq.modules.dataanalysis.domain.vo.res.DataAnalysisAnalyzeRes;
import cn.yq.modules.dataanalysis.domain.vo.res.DataAnalysisPlanRes;

public interface DataAnalysisService {

    DataAnalysisPlanRes plan(DataAnalysisPlanReq request);

    DataAnalysisAnalyzeRes analyze(DataAnalysisAnalyzeReq request);
}
