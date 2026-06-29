package top.xiamoi.moocpass.solver;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 题库解题器工厂（策略模式调度）
 */
@Component
public class QuestionSolverFactory {

    private final Map<String, QuestionSolver> solverMap = new HashMap<>();

    public QuestionSolverFactory(List<QuestionSolver> solvers) {
        for (QuestionSolver solver : solvers) {
            solverMap.put(solver.getProviderCode().toUpperCase(), solver);
        }
    }

    /**
     * 根据题库提供者代码获取解题器
     */
    public QuestionSolver getSolver(String providerCode) {
        if (providerCode == null) {
            return solverMap.get("AI");
        }
        QuestionSolver solver = solverMap.get(providerCode.toUpperCase());
        if (solver == null) {
            return solverMap.get("AI");
        }
        return solver;
    }
}
