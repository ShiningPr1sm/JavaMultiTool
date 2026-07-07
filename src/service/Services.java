package service;

public record Services(
    AuthService authService,
    AchievementService achievementService,
    LevelService levelService,
    WorkflowService workflowService,
    SystemInfoService systemInfoService,
    BDaysService bdaysService,
    GreetingService greetingService,
    RunningProcessService runningProcessService,
    UserSession userSession
) {}
