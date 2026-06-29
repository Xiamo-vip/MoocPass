package top.xiamoi.moocpass.platform.impl;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import top.xiamoi.moocpass.entity.CourseTask;
import top.xiamoi.moocpass.entity.UserPlatformConfig;
import top.xiamoi.moocpass.entity.UserQuestionConfig;
import top.xiamoi.moocpass.mapper.CourseTaskMapper;
import top.xiamoi.moocpass.platform.MoocPlatformAdapter;
import top.xiamoi.moocpass.solver.QuestionSolver;
import top.xiamoi.moocpass.task.TaskLogger;
import top.xiamoi.moocpass.util.OkHttpUtil;
import top.xiamoi.moocpass.vo.CourseVO;
import top.xiamoi.moocpass.service.QuestionCacheService;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 超星学习通平台刷课服务实现
 */
@Slf4j
@Component
public class ChaoxingPlatformAdapter implements MoocPlatformAdapter {

    private static final String PLATFORM_CODE = "chaoxing";
    private static final String PLATFORM_NAME = "超星学习通";
    private static final String AES_KEY = "u2oh6Vu^HWe4_AES";

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36";
    private static final String VIDEO_REFERER =
            "https://mooc1.chaoxing.com/ananas/modules/video/index.html?v=2025-0725-1842";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    @Autowired
    private CourseTaskMapper courseTaskMapper;

    @Autowired
    private QuestionCacheService questionCacheService;

    @Value("${captcha.decoder-url:http://localhost:5000}")
    private String captchaDecoderUrl;

    private final RateLimiter generalRateLimiter = new RateLimiter(500);
    private final RateLimiter videoLogRateLimiter = new RateLimiter(2000);


    private void saveProgress(CourseTask task, int progress, String chapterName) {
        if (task == null) return;
        task.setProgress(progress);
        if (StringUtils.hasText(chapterName)) {
            task.setCurrentChapter(chapterName);
        }
        task.setUpdateTime(java.time.LocalDateTime.now());
        if (courseTaskMapper != null) {
            try {
                courseTaskMapper.updateById(task);
            } catch (Exception e) {
                log.error("保存挂机进度到数据库失败: {}", e.getMessage());
            }
        }
    }

    // ─────────────────────────────── 平台基础信息 ────────────────────────────── //

    @Override
    public String getPlatformCode() { return PLATFORM_CODE; }

    @Override
    public String getPlatformName() { return PLATFORM_NAME; }

    // ─────────────────────────────── 账号验证 ────────────────────────────── //

    @Override
    public boolean validateAccount(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) return false;
        try {
            OkHttpClient client = buildClient();
            return doLogin(client, username, password);
        } catch (Exception e) {
            log.error("超星验证账号失败：", e);
            return false;
        }
    }

    @Override
    public boolean validateAuth(String token) {
        return false;
    }

    // ─────────────────────────────── 获取课程列表 ────────────────────────────── //

    @Override
    public List<CourseVO> getCourseList(UserPlatformConfig config) {
        List<CourseVO> list = new ArrayList<>();
        if (config == null) return list;
        String username = config.getUsername();
        String password = config.getPassword();
        try {
            OkHttpClient client = buildClient();
            if (!doLogin(client, username, password))
                throw new RuntimeException("学习通登录失败，请检查账号密码");
            Request request = new Request.Builder()
                    .url("https://mooc1-api.chaoxing.com/mycourse/backclazzdata?view=json&getTchClazzType=1&mcode=")
                    .header("User-Agent", USER_AGENT)
                    .get().build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonNode root = objectMapper.readTree(response.body().string());
                    JsonNode channelList = root.path("channelList");
                    if (channelList.isArray()) {
                        for (JsonNode channel : channelList) {
                            JsonNode content = channel.path("content");
                            if (content.isMissingNode() || content.has("folderName")) continue;
                            JsonNode courseData = content.path("course").path("data");
                            if (!courseData.isArray()) continue;
                            for (JsonNode item : courseData) {
                                String squareUrl = item.path("courseSquareUrl").asText("");
                                String courseId = item.path("id").asText("");
                                if (!StringUtils.hasText(courseId)) courseId = item.path("courseId").asText("");
                                if (!StringUtils.hasText(courseId)) courseId = extractUrlParam(squareUrl, "courseId");

                                String classId = content.path("id").asText("");
                                if (!StringUtils.hasText(classId)) classId = content.path("key").asText("");
                                if (!StringUtils.hasText(classId)) classId = extractUrlParam(squareUrl, "classId");

                                if (!StringUtils.hasText(courseId)) continue;
                                list.add(CourseVO.builder()
                                        .courseId(courseId)
                                        .classId(classId)
                                        .name(item.path("name").asText(""))
                                        .teacher(item.path("teacherfactor").asText(""))
                                        .coverUrl(item.path("imageurl").asText(""))
                                        .build());
                            }
                        }
                    }
                }
            }

            if (list.isEmpty()) {
                String[] fallbackUrls = new String[]{
                        "https://mooc1-1.chaoxing.com/visit/courselistdata",
                        "https://mooc1-2.chaoxing.com/visit/courselistdata",
                        "https://mooc1.chaoxing.com/visit/courselistdata"
                };
                for (String fallbackUrl : fallbackUrls) {
                    try {
                        Request htmlReq = new Request.Builder()
                                .url(fallbackUrl)
                                .header("User-Agent", USER_AGENT)
                                .get().build();
                        try (Response htmlResp = client.newCall(htmlReq).execute()) {
                            if (htmlResp.isSuccessful() && htmlResp.body() != null) {
                                Document doc = Jsoup.parse(htmlResp.body().string());
                                Elements courseElems = doc.select("li.course, div.course, li.courseItem, div.courseItem");
                                for (Element elem : courseElems) {
                                    String courseId = elem.attr("courseid");
                                    if (!StringUtils.hasText(courseId)) courseId = elem.attr("data-courseid");
                                    String classId = elem.attr("clazzid");
                                    if (!StringUtils.hasText(classId)) classId = elem.attr("data-clazzid");
                                    String name = elem.select("span.course-name, h3.clearfix, a.course-name, h3, div.name").text();
                                    String teacher = elem.select("p.teacher, span.teacher, div.teacher").text();
                                    String coverUrl = elem.select("img").attr("src");
                                    if (StringUtils.hasText(courseId)) {
                                        list.add(CourseVO.builder()
                                                .courseId(courseId)
                                                .classId(classId)
                                                .name(name)
                                                .teacher(teacher)
                                                .coverUrl(coverUrl)
                                                .build());
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                    if (!list.isEmpty()) break;
                }
            }


        } catch (Exception e) {
            log.error("获取超星课程列表失败：", e);
            throw new RuntimeException("获取超星课程列表失败：" + e.getMessage());
        }
        return list;
    }


    // ─────────────────────────────── 执行刷课任务 ────────────────────────────── //

    @Override
    public void executeCourseTask(CourseTask task, UserPlatformConfig platformConfig,
                                  UserQuestionConfig questionConfig,
                                  QuestionSolver questionSolver, TaskLogger taskLogger) {
        taskLogger.log(task.getId(), "正在启动超星学习通刷课引擎...");

        try {
            OkHttpClient client = buildClient();
            boolean loginOk = doLogin(client, platformConfig.getUsername(), platformConfig.getPassword());
            if (!loginOk) {
                taskLogger.log(task.getId(), "账号认证失败，任务终止！");
                throw new RuntimeException("账号认证失败");
            }

            // 获取 UID（用于 enc 计算）
            String userId = getCookieValue(client, "https://passport2.chaoxing.com", "_uid");
            if (!StringUtils.hasText(userId))
                userId = getCookieValue(client, "https://passport2.chaoxing.com", "UID");
            if (!StringUtils.hasText(userId)) userId = "0";

            taskLogger.log(task.getId(), "账号登录成功 (UID: " + userId + ")");
            taskLogger.log(task.getId(), "正在拉取课程 [" + task.getCourseName() + "] 章节结构...");

            // 获取 fid（用于视频状态请求的 k 参数）
            String fid = fetchFid(client);

            String cpi = fetchCpi(client, task.getCourseId(), task.getClassId());
            List<KnowledgePoint> points = fetchKnowledgePoints(client, task.getCourseId(), task.getClassId(), cpi);
            taskLogger.log(task.getId(), "章节解析完成，共 " + points.size() + " 个小节。");

            if (points.isEmpty()) {
                taskLogger.log(task.getId(), "未找到可用章节，可能课程尚未开放。");
                task.setProgress(100);
                task.setStatus("COMPLETED");
                return;
            }

            int totalPoints = points.size();
            java.util.concurrent.atomic.AtomicInteger globalDoneJobsCounter = new java.util.concurrent.atomic.AtomicInteger(0);

            for (int i = 0; i < totalPoints; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    taskLogger.log(task.getId(), "任务已暂停/停止。");
                    return;
                }

                KnowledgePoint point = points.get(i);
                taskLogger.log(task.getId(), String.format("[%d/%d] 正在解析章节: %s", i + 1, totalPoints, point.title));
                JobFetchResult jobResult = fetchJobs(client, task, point, cpi, fid, taskLogger);

                if (jobResult.jobs.isEmpty()) {
                    if (jobResult.hasAnyCards) {
                        taskLogger.log(task.getId(), "[跳过] 本章节所有任务点已完成。");
                    } else {
                        taskLogger.log(task.getId(), "[完成] 本章节无任务点（纯内容页），已自动标记完成。");
                    }
                    int sectionProgress = Math.min(99, (int) (((double) (i + 1) / totalPoints) * 100));
                    saveProgress(task, sectionProgress, String.format("[%d/%d] %s", i + 1, totalPoints, point.title));
                    continue;
                }

                final int sectionIndex = i;
                final int totalSections = totalPoints;
                final int sectionJobs = jobResult.jobs.size();
                java.util.concurrent.atomic.AtomicInteger sectionDoneCounter = new java.util.concurrent.atomic.AtomicInteger(0);
                final String currentUserId = userId;

                for (JobPoint job : jobResult.jobs) {
                    if (Thread.currentThread().isInterrupted()) return;
                    processSingleJob(client, task, job, cpi, fid, currentUserId, jobResult, questionConfig, questionSolver, taskLogger);
                    int doneInSection = sectionDoneCounter.incrementAndGet();
                    globalDoneJobsCounter.incrementAndGet();

                    double courseProgressDouble = ((double) sectionIndex + ((double) doneInSection / sectionJobs)) / totalSections * 100.0;
                    int overallProgress = Math.min(99, Math.max(1, (int) courseProgressDouble));
                    String chapterStatus = String.format("[%d/%d] %s (本节任务点 %d/%d)", sectionIndex + 1, totalSections, point.title, doneInSection, sectionJobs);

                    saveProgress(task, overallProgress, chapterStatus);
                    taskLogger.log(task.getId(), String.format("[全课总进度] 完成率: %d%% | 正在处理: %s", overallProgress, chapterStatus));
                }

            }

            saveProgress(task, 100, "全课完成");
            task.setStatus("COMPLETED");
            taskLogger.log(task.getId(), "课程 [" + task.getCourseName() + "] 刷课任务全部完成！");

        } catch (Exception e) {
            log.error("刷课任务异常：", e);
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            taskLogger.log(task.getId(), "刷课任务失败：" + e.getMessage());
        }
    }

    private void processSingleJob(OkHttpClient client, CourseTask task, JobPoint job,
                                 String cpi, String fid, String userId, JobFetchResult jobResult,
                                 UserQuestionConfig questionConfig, QuestionSolver questionSolver,
                                 TaskLogger taskLogger) {
        taskLogger.log(task.getId(), String.format("[任务点] 开始处理 [%s] (类型: %s)", job.name, job.type));
        try {
            switch (job.type.toLowerCase()) {
                case "video":
                    studyVideo(client, task, job, cpi, fid, userId, taskLogger);
                    break;
                case "audio":
                    studyVideo(client, task, job, cpi, fid, userId, taskLogger); // 音频与视频共享底层播放上报逻辑
                    break;
                case "document":
                    studyDocument(client, task, job, taskLogger);
                    break;
                case "workid":
                    studyWork(client, task, job, jobResult.jobInfo, questionConfig, questionSolver, taskLogger);
                    break;
                case "read":
                    studyRead(client, task, job, jobResult.jobInfo, taskLogger);
                    break;
                default:
                    taskLogger.log(task.getId(), "[跳过] 未知任务类型 [" + job.type + "]。");
            }
            sleepRandom(300, 600);
        } catch (Exception e) {
            log.error("处理任务点异常 [job={}]:", job.name, e);
            taskLogger.log(task.getId(), "[异常] 任务点 [" + job.name + "] 处理失败: " + e.getMessage());
        }
    }

    // ─────────────────────────────── 视频任务 ────────────────────────────── //
    private void studyVideo(OkHttpClient client, CourseTask task, JobPoint job,
                            String cpi, String fid, String userId, TaskLogger taskLogger) {
        try {
            // 1. 获取视频状态
            // k 参数 = fid
            String statusUrl = String.format(
                    "https://mooc1.chaoxing.com/ananas/status/%s?k=%s&flag=normal",
                    job.objectId, fid);
            Request statusReq = new Request.Builder()
                    .url(statusUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("Referer", VIDEO_REFERER)
                    .get().build();

            int duration = 60;
            String dtoken = "";
            String rt = job.rt; // 从卡片数据带入
            String videoName = StringUtils.hasText(job.name) ? job.name : "未命名视频";
            try (Response resp = client.newCall(statusReq).execute()) {
                if (!resp.isSuccessful() || resp.body() == null) {
                    taskLogger.log(task.getId(), String.format("[视频] 《%s》 获取视频状态失败，跳过。", videoName));
                    return;
                }
                JsonNode statusJson = objectMapper.readTree(resp.body().string());
                if (!"success".equals(statusJson.path("status").asText())) {
                    taskLogger.log(task.getId(), String.format("[视频] 《%s》 视频状态异常，跳过。", videoName));
                    return;
                }
                dtoken   = statusJson.path("dtoken").asText("");
                duration = statusJson.path("duration").asInt(0);
                if (duration == 0) duration = 60;
            }

            double speed = (task.getSpeed() != null) ? task.getSpeed() : 1.0;
            int playTime = job.playTime / 1000; // 已播放秒数

            taskLogger.log(task.getId(), String.format(
                    "[视频] 《%s》 初始化成功: 总时长 %ds | 初始已播 %ds | 倍速 %.1fx", videoName, duration, playTime, speed));

            // 2. 先尝试 isdrag=4 直接完成
            VideoLogResult instant = reportVideoLog(client, task, job, cpi, userId, dtoken, duration, duration, rt, 4, taskLogger);
            if (instant.isPassed) {
                taskLogger.log(task.getId(), String.format("[视频] 《%s》 任务已完成（秒过秒刷成功）！", videoName));
                return;
            }

            // 3. 模拟播放进度
            int lastLogTime = 0;
            int waitTime = 30 + random.nextInt(60); // 30~90s
            boolean passed = false;
            int forbiddenRetry = 0;
            final int maxForbiddenRetry = 2;
            int endRetryCount = 0;
            long lastIter = System.currentTimeMillis();

            while (!passed) {
                if (Thread.currentThread().isInterrupted()) return;

                if (playTime - lastLogTime >= waitTime || playTime >= duration) {
                    VideoLogResult result = reportVideoLog(client, task, job, cpi, userId,
                            dtoken, duration, playTime, rt, 3, taskLogger);
                    if (result.statusCode == 403) {
                        if (forbiddenRetry >= maxForbiddenRetry) {
                            taskLogger.log(task.getId(), String.format("[视频] 《%s》 403 重试失败，跳过该视频。", videoName));
                            return;
                        }
                        forbiddenRetry++;
                        taskLogger.log(task.getId(), String.format("[视频] 《%s》 遇到 403 报错，尝试恢复会话状态 (第%d次)", videoName, forbiddenRetry));
                        sleepRandom(2000, 4000);
                        JsonNode refreshed = refreshVideoStatus(client, job.objectId, fid);
                        if (refreshed != null && refreshed.has("dtoken")) {
                            dtoken = refreshed.path("dtoken").asText(dtoken);
                        }
                        continue;
                    } else if (!result.isPassed && result.statusCode != 200) {
                        taskLogger.log(task.getId(), String.format("[视频] 《%s》 上报异常(状态码:%d)，跳过该视频。", videoName, result.statusCode));
                        return;
                    }

                    passed = result.isPassed;
                    lastLogTime = playTime;
                    waitTime = 30 + random.nextInt(60);

                    taskLogger.log(task.getId(), String.format(
                            "[视频] 《%s》 播放进度上报: %ds / %ds (%d%%)",
                            videoName, playTime, duration, Math.min(100, playTime * 100 / Math.max(1, duration))));

                    if (passed) {
                        break;
                    } else if (playTime >= duration) {
                        endRetryCount++;
                        taskLogger.log(task.getId(), String.format("[视频] 《%s》 播放已达终点，正在等待超星服务端完成结算 (第%d次验证)...", videoName, endRetryCount));
                        if (endRetryCount % 2 == 0) {
                            JsonNode statusObj = refreshVideoStatus(client, job.objectId, fid);
                            if (statusObj != null) {
                                if ("success".equals(statusObj.path("status").asString()) && statusObj.path("isPassed").asBoolean(false)) {
                                    break;
                                }
                                if (statusObj.has("dtoken")) {
                                    dtoken = statusObj.path("dtoken").asText(dtoken);
                                }
                            }
                        }
                        sleepRandom(1500, 2500);
                    }
                }

                sleepRandom(950, 1050);
                long now = System.currentTimeMillis();
                double dt = ((now - lastIter) / 1000.0) * speed;
                lastIter = now;
                playTime = Math.min(duration, playTime + (int) Math.max(1, Math.round(dt)));
            }


            taskLogger.log(task.getId(), String.format("[视频] 《%s》 视频任务学习完成！", videoName));
        } catch (Exception e) {
            log.error("视频任务异常：", e);
            String vName = (job != null && StringUtils.hasText(job.name)) ? job.name : "未命名视频";
            taskLogger.log(task.getId(), String.format("[视频] 《%s》 视频任务异常：%s", vName, e.getMessage()));
        }
    }

    private JsonNode refreshVideoStatus(OkHttpClient client, String objectId, String fid) {
        try {
            String infoUrl = String.format("https://mooc1.chaoxing.com/ananas/status/%s?k=%s&flag=normal", objectId, fid);
            Request req = new Request.Builder()
                    .url(infoUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("Referer", VIDEO_REFERER)
                    .get().build();
            try (Response resp = client.newCall(req).execute()) {
                if (resp.isSuccessful() && resp.body() != null) {
                    return objectMapper.readTree(resp.body().string());
                }
            }
        } catch (Exception e) {
            log.warn("刷新视频状态异常: {}", e.getMessage());
        }
        return null;
    }

    private VideoLogResult reportVideoLog(OkHttpClient client, CourseTask task, JobPoint job,

                                          String cpi, String userId, String dtoken,
                                          int duration, int playingTime, String rt, int isdrag,
                                          TaskLogger taskLogger) {
        try {
            videoLogRateLimiter.limitRate(true, 0, 2000);
            String rawSign = String.format("[%s][%s][%s][%s][%d][d_yHJ!$pdA~5][%d][0_%d]",
                    task.getClassId(), userId, job.jobId, job.objectId,
                    (long) playingTime * 1000, (long) duration * 1000, duration);
            String enc = DigestUtils.md5DigestAsHex(rawSign.getBytes(StandardCharsets.UTF_8));

            HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(
                            "https://mooc1.chaoxing.com/mooc-ans/multimedia/log/a/" + cpi + "/" + dtoken)).newBuilder()
                    .addQueryParameter("clazzId",    task.getClassId())
                    .addQueryParameter("playingTime", String.valueOf(playingTime))
                    .addQueryParameter("duration",    String.valueOf(duration))
                    .addQueryParameter("clipTime",    "0_" + duration)
                    .addQueryParameter("objectId",    job.objectId)
                    .addQueryParameter("otherInfo",   job.otherInfo)
                    .addQueryParameter("courseId",    task.getCourseId())
                    .addQueryParameter("jobid",       job.jobId)
                    .addQueryParameter("userid",      userId)
                    .addQueryParameter("isdrag",      String.valueOf(isdrag))
                    .addQueryParameter("view",        "pc")
                    .addQueryParameter("enc",         enc)
                    .addQueryParameter("dtype",       "Video");

            if (StringUtils.hasText(job.videoFaceCaptureEnc))
                urlBuilder.addQueryParameter("videoFaceCaptureEnc", job.videoFaceCaptureEnc);
            if (StringUtils.hasText(job.attDuration))
                urlBuilder.addQueryParameter("attDuration", job.attDuration);
            if (StringUtils.hasText(job.attDurationEnc))
                urlBuilder.addQueryParameter("attDurationEnc", job.attDurationEnc);

            String resolvedRt = resolveRt(rt, job.otherInfo);

            if (StringUtils.hasText(resolvedRt)) {
                urlBuilder.addQueryParameter("rt", resolvedRt)
                        .addQueryParameter("_t", String.valueOf(System.currentTimeMillis()));

                Request req = new Request.Builder()
                        .url(urlBuilder.build())
                        .header("User-Agent", USER_AGENT)
                        .header("Referer", VIDEO_REFERER)
                        .get().build();

                try (Response resp = client.newCall(req).execute()) {
                    return parseVideoLogResponse(client, req, resp, taskLogger, task != null ? task.getId() : null);
                }
            } else {
                for (String tryRt : new String[]{"0.9", "1"}) {
                    HttpUrl url = urlBuilder
                            .removeAllQueryParameters("rt")
                            .removeAllQueryParameters("_t")
                            .addQueryParameter("rt", tryRt)
                            .addQueryParameter("_t", String.valueOf(System.currentTimeMillis()))
                            .build();

                    Request req = new Request.Builder()
                            .url(url)
                            .header("User-Agent", USER_AGENT)
                            .header("Referer", VIDEO_REFERER)
                            .get().build();

                    try (Response resp = client.newCall(req).execute()) {
                        VideoLogResult res = parseVideoLogResponse(client, req, resp, taskLogger, task != null ? task.getId() : null);
                        if (res.statusCode == 200) return res;
                        if (res.statusCode == 403) {
                            log.warn("视频上报 rt={} 返回 403，尝试切换", tryRt);
                        }
                    }
                }
                return new VideoLogResult(false, 403);
            }
        } catch (Exception e) {
            log.error("视频进度上报异常：", e);
            return new VideoLogResult(false, -1);
        }
    }

    private String resolveRt(String rt, String otherInfo) {
        if (StringUtils.hasText(rt)) return rt;
        if (StringUtils.hasText(otherInfo)) {
            Matcher m = Pattern.compile("-rt_([1d])").matcher(otherInfo);
            if (m.find()) {
                return "d".equals(m.group(1)) ? "0.9" : "1";
            }
        }
        return "";
    }

    private VideoLogResult parseVideoLogResponse(OkHttpClient client, Request originalReq, Response resp, TaskLogger taskLogger, Long taskId) throws Exception {
        if (resp.body() != null) {
            String bodyStr = resp.body().string();
            if (checkNeedCaptcha(bodyStr)) {
                boolean solved = handleCaptchaPass(client, taskLogger, taskId);
                if (solved) {
                    try (Response retryResp = client.newCall(originalReq).execute()) {
                        if (retryResp.isSuccessful() && retryResp.body() != null) {
                            JsonNode json = objectMapper.readTree(retryResp.body().string());
                            return new VideoLogResult(json.path("isPassed").asBoolean(false), 200);
                        }
                    }
                }
            }
            if (resp.isSuccessful()) {
                try {
                    JsonNode json = objectMapper.readTree(bodyStr);
                    return new VideoLogResult(json.path("isPassed").asBoolean(false), 200);
                } catch (Exception ignored) {}
            }
        }
        return new VideoLogResult(false, resp.code());
    }


    // ─────────────────────────────── 文档任务 ────────────────────────────── //
    private void studyDocument(OkHttpClient client, CourseTask task, JobPoint job, TaskLogger taskLogger) {
        try {
            String knowledgeid = extractNodeId(job.otherInfo);

            HttpUrl.Builder urlBuilder = Objects.requireNonNull(
                            HttpUrl.parse("https://mooc1.chaoxing.com/ananas/job/document")).newBuilder()
                    .addQueryParameter("jobid",       job.jobId)
                    .addQueryParameter("knowledgeid", knowledgeid)
                    .addQueryParameter("courseid",    task.getCourseId())
                    .addQueryParameter("clazzid",     task.getClassId())
                    .addQueryParameter("_dc",         String.valueOf(System.currentTimeMillis()));
            if (StringUtils.hasText(job.jtoken))
                urlBuilder.addQueryParameter("jtoken", job.jtoken);

            Request request = new Request.Builder()
                    .url(urlBuilder.build())
                    .header("User-Agent", USER_AGENT)
                    .get().build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    taskLogger.log(task.getId(), "[文档] 文档任务点打卡成功！");
                } else {
                    taskLogger.log(task.getId(), "[文档] 文档任务点打卡失败，状态码：" + response.code());
                }
            }
        } catch (Exception e) {
            taskLogger.log(task.getId(), "[文档] 文档任务处理异常：" + e.getMessage());
        }
    }

    // ─────────────────────────────── 阅读任务 ────────────────────────────── //
    private void studyRead(OkHttpClient client, CourseTask task, JobPoint job,
                           JobInfo jobInfo, TaskLogger taskLogger) {
        try {
            HttpUrl url = Objects.requireNonNull(
                            HttpUrl.parse("https://mooc1.chaoxing.com/ananas/job/readv2")).newBuilder()
                    .addQueryParameter("jobid",       job.jobId)
                    .addQueryParameter("knowledgeid", jobInfo.knowledgeid)
                    .addQueryParameter("jtoken",      job.jtoken)
                    .addQueryParameter("courseid",    task.getCourseId())
                    .addQueryParameter("clazzid",     task.getClassId())
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .get().build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonNode json = objectMapper.readTree(response.body().string());
                    taskLogger.log(task.getId(), "[阅读] 阅读任务完成：" + json.path("msg").asText());
                } else {
                    taskLogger.log(task.getId(), "[阅读] 阅读任务失败，状态码：" + response.code());
                }
            }
        } catch (Exception e) {
            taskLogger.log(task.getId(), "[阅读] 阅读任务处理异常：" + e.getMessage());
        }
    }

    // ─────────────────────────────── 作业任务 ────────────────────────────── //
    private void studyWork(OkHttpClient client, CourseTask task, JobPoint job,
                           JobInfo jobInfo, UserQuestionConfig questionConfig,
                           QuestionSolver questionSolver, TaskLogger taskLogger) {
        taskLogger.log(task.getId(), "[测验] 正在拉取测验题目...");

        try {
            String workId = job.jobId.replace("work-", "");
            HttpUrl.Builder urlBuilder = Objects.requireNonNull(
                            HttpUrl.parse("https://mooc1.chaoxing.com/mooc-ans/api/work")).newBuilder()
                    .addQueryParameter("api",           "1")
                    .addQueryParameter("workId",        workId)
                    .addQueryParameter("jobid",         job.jobId)
                    .addQueryParameter("originJobId",   job.jobId)
                    .addQueryParameter("needRedirect",  "true")
                    .addQueryParameter("skipHeader",    "true")
                    .addQueryParameter("cpi",           (jobInfo != null && jobInfo.cpi != null) ? jobInfo.cpi : "")
                    .addQueryParameter("ut",            "s")
                    .addQueryParameter("clazzId",       task.getClassId())
                    .addQueryParameter("type",          "")
                    .addQueryParameter("enc",           job.enc != null ? job.enc : "")
                    .addQueryParameter("mooc2",         "1")
                    .addQueryParameter("courseid",      task.getCourseId());

            if (jobInfo != null) {
                if (StringUtils.hasText(jobInfo.knowledgeid)) {
                    urlBuilder.addQueryParameter("knowledgeid", jobInfo.knowledgeid);
                }
                if (StringUtils.hasText(jobInfo.ktoken)) {
                    urlBuilder.addQueryParameter("ktoken", jobInfo.ktoken);
                }
            }

            HttpUrl workUrl = urlBuilder.build();
            Request workReq = new Request.Builder()
                    .url(workUrl)
                    .header("User-Agent", USER_AGENT)
                    .get().build();
            String html = executeStringWithCaptchaCheck(client, workReq, taskLogger, task.getId());
            if (html != null && html.contains("教师未创建完成该测验")) {
                taskLogger.log(task.getId(), "教师未创建完成该测验，跳过。");
                return;
            }

            if (html == null) {
                taskLogger.log(task.getId(), "拉取题目失败，已跳过。");
                return;
            }

            WorkQuestions workData = parseWorkHtml(html);
            if (workData == null || workData.questions.isEmpty()) {
                taskLogger.log(task.getId(), "未解析到题目内容，跳过。");
                return;
            }

            taskLogger.log(task.getId(), "共解析到 " + workData.questions.size() + " 道题目");

            boolean autoAnswer = task.getAutoAnswer() == null || task.getAutoAnswer();
            if (!autoAnswer) {
                taskLogger.log(task.getId(), "用户配置已关闭自动答题，跳过此作业项。");
                return;
            }

            boolean shouldSubmit = task.getSubmitAnswer() != null ? task.getSubmitAnswer()
                    : (questionConfig != null && Boolean.TRUE.equals(questionConfig.getSubmit()));
            double coverRate = task.getCoverRate() != null ? task.getCoverRate()
                    : ((questionConfig != null && questionConfig.getCoverRate() != null) ? questionConfig.getCoverRate() : 0.8);

            int found = 0;
            for (WorkQuestion q : workData.questions) {
                String answer = "";
                String qType = StringUtils.hasText(q.rawAnswerType) ? q.rawAnswerType : q.type;

                // 1. 优先从本地客观题缓存查找
                if (questionCacheService != null) {
                    answer = questionCacheService.findCachedAnswer(q.title, qType);
                    if (StringUtils.hasText(answer)) {
                        found++;
                        taskLogger.log(task.getId(), "[缓存命中] [" + q.type + "] " + truncate(q.title, 25));
                    }
                }

                // 2. 未命中缓存时调用 AI 大模型搜题
                if (!StringUtils.hasText(answer) && questionSolver != null && questionConfig != null) {
                    try {
                        answer = questionSolver.solve(q.title, q.options, questionConfig);
                        if (StringUtils.hasText(answer)) {
                            found++;
                            // 保存客观题答案到缓存
                            if (questionCacheService != null) {
                                questionCacheService.saveAnswerCache(q.title, qType, answer);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("搜题失败：{}", e.getMessage());
                    }
                }

                if (!StringUtils.hasText(answer)) {
                    answer = randomAnswer(q);
                }

                if ("2".equals(q.rawAnswerType) || "completion".equals(q.type)) {
                    workData.formData.remove("answer" + q.id);
                    int count = Math.max(1, q.completionCount);
                    String[] ansParts = StringUtils.hasText(answer) ? splitCompletionAnswer(answer, count) : new String[0];
                    workData.formData.put("tiankongsize" + q.id, String.valueOf(count));
                    for (int i = 1; i <= count; i++) {
                        String subAns = (i <= ansParts.length && StringUtils.hasText(ansParts[i - 1]))
                                ? ansParts[i - 1].trim() : "略";
                        subAns = subAns.replaceAll("^(?:\\d+|[一二三四五六七八九十]+|[①②③④⑤⑥⑦⑧⑨⑩])\\s*[.、:：]?\\s*", "");
                        if (!StringUtils.hasText(subAns)) subAns = "略";
                        if (!subAns.startsWith("<p>")) subAns = "<p>" + subAns + "</p>";
                        workData.formData.put("answerEditor" + q.id + i, subAns);
                    }
                    taskLogger.log(task.getId(), "[答题] [填空题] " + truncate(q.title, 30) + " → " + answer);
                } else if ("4".equals(q.rawAnswerType) || "subjective".equals(q.type)) {
                    String finalAns = StringUtils.hasText(answer) ? answer.trim() : "略";
                    if (!finalAns.startsWith("<p>")) finalAns = "<p>" + finalAns + "</p>";
                    workData.formData.put("answer" + q.id, finalAns);
                    taskLogger.log(task.getId(), "[答题] [主观题] " + truncate(q.title, 30) + " → " + finalAns);
                } else if ("judgement".equals(q.type) || "3".equals(q.rawAnswerType)) {
                    String normAns = normalizeJudgementAnswer(answer);
                    workData.formData.put("answer" + q.id, normAns);
                    taskLogger.log(task.getId(), "[答题] [判断题] " + truncate(q.title, 30) + " → " + normAns);
                } else if ("multiple".equals(q.type) || "1".equals(q.rawAnswerType)) {
                    String normAns = normalizeMultipleAnswer(answer);
                    workData.formData.put("answer" + q.id, normAns);
                    taskLogger.log(task.getId(), "[答题] [多选题] " + truncate(q.title, 30) + " → " + normAns);
                } else {
                    String normAns = normalizeSingleAnswer(answer);
                    workData.formData.put("answer" + q.id, normAns);
                    taskLogger.log(task.getId(), "[答题] [" + q.type + "] " + truncate(q.title, 30) + " → " + normAns);
                }
            }

            double actualCoverRate = workData.questions.isEmpty() ? 0.0
                    : (double) found / workData.questions.size();
            boolean doSubmit = shouldSubmit && (actualCoverRate >= coverRate);
            if (!doSubmit) {
                workData.formData.put("pyFlag", "1");
                taskLogger.log(task.getId(), String.format(
                        "[答题] 覆盖率 %.0f%% (阈值 %.0f%%)，仅保存不提交。",
                        actualCoverRate * 100, coverRate * 100));
            } else {
                workData.formData.put("pyFlag", "");
            }

            FormBody.Builder formBuilder = new FormBody.Builder();
            for (Map.Entry<String, String> entry : workData.formData.entrySet()) {
                formBuilder.add(entry.getKey(), entry.getValue() != null ? entry.getValue() : "");
            }

            Request submitReq = new Request.Builder()
                    .url("https://mooc1.chaoxing.com/mooc-ans/work/addStudentWorkNew")
                    .header("User-Agent",       "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36 Edg/129.0.0.0")
                    .header("Content-Type",     "application/x-www-form-urlencoded; charset=UTF-8")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("Origin",           "https://mooc1.chaoxing.com")
                    .header("Accept",           "application/json, text/javascript, */*; q=0.01")
                    .post(formBuilder.build())
                    .build();

            try (Response submitResp = client.newCall(submitReq).execute()) {
                if (submitResp.isSuccessful() && submitResp.body() != null) {
                    JsonNode result = objectMapper.readTree(submitResp.body().string());
                    boolean status = result.path("status").asBoolean(false);
                    String msg = result.path("msg").asText("");
                    if (status) {
                        taskLogger.log(task.getId(), "[答题] " + (doSubmit ? "提交" : "保存") + "答题成功：" + msg);
                    } else {
                        taskLogger.log(task.getId(), "[答题] " + (doSubmit ? "提交" : "保存") + "答题失败：" + msg);
                    }
                }
            }

        } catch (Exception e) {
            log.error("作业任务处理异常：", e);
            taskLogger.log(task.getId(), "[答题] 作业任务处理异常：" + e.getMessage());
        }
    }

    // ─────────────────────────────── 登录 ────────────────────────────── //

    private boolean doLogin(OkHttpClient client, String username, String password) throws Exception {
        FormBody formBody = new FormBody.Builder()
                .add("fid",               "-1")
                .add("uname",             aesEncrypt(username, AES_KEY))
                .add("password",          aesEncrypt(password, AES_KEY))
                .add("refer",             "https://i.chaoxing.com")
                .add("t",                 "true")
                .add("forbidotherlogin",  "0")
                .add("validate",          "")
                .add("doubleFactorLogin", "0")
                .add("independentId",     "0")
                .build();

        Request request = new Request.Builder()
                .url("https://passport2.chaoxing.com/fanyalogin")
                .header("User-Agent", USER_AGENT)
                .post(formBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String body = response.body().string();
                JsonNode json = objectMapper.readTree(body);
                boolean status = json.path("status").asBoolean(false);
                if (!status) {
                    log.warn("超星登录失败：{}", body);
                    return false;
                }
                // 登录成功后必须访问子域名激活全局 Cookie 会话（解决 mooc1-api 请重新登录问题）
                try {
                    Request initReq1 = new Request.Builder()
                            .url("https://i.chaoxing.com/base?refer=https://i.chaoxing.com")
                            .header("User-Agent", USER_AGENT)
                            .get().build();
                    try (Response r1 = client.newCall(initReq1).execute()) {}

                    Request initReq2 = new Request.Builder()
                            .url("https://mooc1-1.chaoxing.com/visit/courselistdata")
                            .header("User-Agent", USER_AGENT)
                            .get().build();
                    try (Response r2 = client.newCall(initReq2).execute()) {}
                } catch (Exception ignored) {}
                return true;
            }
        }
        return false;

    }

    // ─────────────────────────────── 数据获取 ────────────────────────────── //

    private String fetchFid(OkHttpClient client) {
        try {
            Request req = new Request.Builder()
                    .url("https://i.chaoxing.com/base?refer=https://i.chaoxing.com")
                    .header("User-Agent", USER_AGENT)
                    .get().build();
            try (Response resp = client.newCall(req).execute()) {
                if (resp.isSuccessful() && resp.body() != null) {
                    String html = resp.body().string();
                    Matcher m = Pattern.compile("\"fid\"\\s*:\\s*\"([^\"]+)\"").matcher(html);
                    if (m.find()) return m.group(1);
                    String fid = getCookieValue(client, "https://i.chaoxing.com", "fid");
                    if (StringUtils.hasText(fid)) return fid;
                }
            }
        } catch (Exception e) {
            log.warn("获取 fid 失败：{}", e.getMessage());
        }
        return "";
    }

    private String fetchCpi(OkHttpClient client, String courseId, String classId) {
        try {
            String url = String.format(
                    "https://mooc2-ans.chaoxing.com/mooc2-ans/mycourse/studentcourse?courseid=%s&clazzid=%s&ut=s",
                    courseId, classId);
            Request req = new Request.Builder().url(url).header("User-Agent", USER_AGENT).get().build();
            try (Response resp = client.newCall(req).execute()) {
                if (resp.isSuccessful() && resp.body() != null) {
                    Matcher m = Pattern.compile("cpi=([0-9]+)").matcher(resp.body().string());
                    if (m.find()) return m.group(1);
                }
            }
        } catch (Exception e) {
            log.warn("解析 cpi 异常：{}", e.getMessage());
        }
        return "0";
    }

    private List<KnowledgePoint> fetchKnowledgePoints(OkHttpClient client, String courseId, String classId, String cpi) {
        List<KnowledgePoint> points = new ArrayList<>();
        try {
            String url = String.format(
                    "https://mooc2-ans.chaoxing.com/mooc2-ans/mycourse/studentcourse?courseid=%s&clazzid=%s&cpi=%s&ut=s",
                    courseId, classId, cpi);
            Request req = new Request.Builder().url(url).header("User-Agent", USER_AGENT).get().build();
            String html = executeStringWithCaptchaCheck(client, req, null, null);
            if (!StringUtils.hasText(html)) return points;
                Pattern pat = Pattern.compile(
                        "id=\"cur([0-9]+)\"[^>]*>.*?<a[^>]*class=\"clicktitle\"[^>]*>(.*?)</a>",
                        Pattern.DOTALL);
                Matcher m = pat.matcher(html);
                while (m.find()) {
                    KnowledgePoint kp = new KnowledgePoint();
                    kp.id    = m.group(1);
                    kp.title = m.group(2).replaceAll("<[^>]+>", "").trim();
                    points.add(kp);
                }

                if (points.isEmpty()) {
                    // 备用正则
                    Pattern fallback = Pattern.compile("id=\"node([0-9]+)\"[^>]*title=\"([^\"]+)\"");
                    Matcher fm = fallback.matcher(html);
                    while (fm.find()) {
                        KnowledgePoint kp = new KnowledgePoint();
                        kp.id    = fm.group(1);
                        kp.title = fm.group(2).trim();
                        points.add(kp);
                    }
                }
        } catch (Exception e) {
            log.error("提取章节列表异常：", e);
        }
        return points;
    }


    private JobFetchResult fetchJobs(OkHttpClient client, CourseTask task,
                                     KnowledgePoint point, String cpi, String fid,
                                     TaskLogger taskLogger) {
        List<JobPoint> jobs = new ArrayList<>();
        JobInfo jobInfo = new JobInfo();
        boolean hasAnyCards = false;
        try {
            for (int num = 0; num <= 6; num++) {
                HttpUrl url = Objects.requireNonNull(
                                HttpUrl.parse("https://mooc1.chaoxing.com/mooc-ans/knowledge/cards")).newBuilder()
                        .addQueryParameter("clazzid",    task.getClassId())
                        .addQueryParameter("courseid",   task.getCourseId())
                        .addQueryParameter("knowledgeid", point.id)
                        .addQueryParameter("ut",         "s")
                        .addQueryParameter("cpi",        cpi)
                        .addQueryParameter("num",        String.valueOf(num))
                        .addQueryParameter("v",          "2025-0424-1038-3")
                        .addQueryParameter("mooc2",      "1")
                        .build();
                Request req = new Request.Builder().url(url).header("User-Agent", USER_AGENT).get().build();
                String html = executeStringWithCaptchaCheck(client, req, taskLogger, task.getId());
                if (!StringUtils.hasText(html)) break;
                if (html.contains("章节未开放")) break;
                    String compactHtml = html.replace(" ", "");
                    Matcher mArgMatcher = Pattern.compile("mArg=\\{(.*?)\\};", Pattern.DOTALL)
                            .matcher(compactHtml);

                    if (!mArgMatcher.find()) continue;

                    String jsonStr = "{" + mArgMatcher.group(1) + "}";
                    JsonNode cardsData;
                    try {
                        cardsData = objectMapper.readTree(jsonStr);
                    } catch (Exception e) {
                        log.warn("mArg JSON 解析失败，num={}: {}", num, e.getMessage());
                        continue;
                    }

                    JsonNode defaults = cardsData.path("defaults");
                    if (!defaults.isMissingNode()) {
                        jobInfo.ktoken      = defaults.path("ktoken").asText("");
                        jobInfo.mtEnc       = defaults.path("mtEnc").asText("");
                        jobInfo.defenc      = defaults.path("defenc").asText("");
                        jobInfo.cardid      = defaults.path("cardid").asText("");
                        jobInfo.cpi         = defaults.path("cpi").asText(cpi);
                        jobInfo.qnenc       = defaults.path("qnenc").asText("");
                        jobInfo.knowledgeid = defaults.path("knowledgeid").asText(point.id);
                        jobInfo.reportTimeInterval = defaults.path("reportTimeInterval").asInt(60);
                    }
                    JsonNode attachments = cardsData.path("attachments");
                    if (!attachments.isArray() || attachments.size() == 0) continue;
                    hasAnyCards = true;

                    for (JsonNode card : attachments) {
                        // 跳过已通过的任务
                        if (card.path("isPassed").asBoolean(false)) continue;

                        String cardType  = card.path("type").asText("").toLowerCase();
                        JsonNode property = card.path("property");
                        String otherInfo = card.path("otherInfo").asText("");
                        if (otherInfo.contains("&")) otherInfo = otherInfo.split("&")[0];
                        String propType      = property.path("type").asText("").toLowerCase();
                        String resourceType  = property.path("resourceType").asText("").toLowerCase();
                        boolean isLive = cardType.contains("live") || propType.contains("live")
                                || resourceType.contains("live")
                                || !property.path("liveId").isMissingNode()
                                || !property.path("streamName").isMissingNode()
                                || !property.path("vdoid").isMissingNode();

                        if (isLive) {
                            taskLogger.log(task.getId(), "  │  ├─ 检测到直播任务，跳过。");
                            continue;
                        }
                        if ("read".equals(cardType) && !property.path("read").asBoolean(false)) {
                            JobPoint jp = new JobPoint();
                            jp.type      = "read";
                            jp.jobId     = card.path("jobid").asText("");
                            jp.jtoken    = card.path("jtoken").asText("");
                            jp.otherInfo = otherInfo;
                            jp.name      = property.path("title").asText("阅读任务");
                            jp.mid       = card.path("mid").asText("");
                            jp.enc       = card.path("enc").asText("");
                            if (StringUtils.hasText(jp.jobId)) jobs.add(jp);
                            continue;
                        }

                        JobPoint jp = new JobPoint();
                        jp.otherInfo = otherInfo;
                        jp.mid       = card.path("mid").asText("");
                        jp.enc       = card.path("enc").asText("");
                        jp.aid       = card.path("aid").asText("");

                        switch (cardType) {
                            case "video":
                            case "audio":
                                jp.type      = cardType;
                                jp.jobId     = card.path("jobid").asText("");
                                jp.objectId  = card.path("objectId").asText("");
                                jp.name      = property.path("name").asText("视频任务");
                                jp.playTime  = card.path("playTime").asInt(0);
                                jp.rt        = property.path("rt").asText("");
                                jp.attDuration        = card.path("attDuration").asText("");
                                jp.attDurationEnc     = card.path("attDurationEnc").asText("");
                                jp.videoFaceCaptureEnc = card.path("videoFaceCaptureEnc").asText("");
                                if (StringUtils.hasText(jp.jobId) && StringUtils.hasText(jp.objectId))
                                    jobs.add(jp);
                                break;

                            case "document":
                                jp.type      = "document";
                                jp.jobId     = card.path("jobid").asText("");
                                jp.jtoken    = card.path("jtoken").asText("");
                                jp.objectId  = property.path("objectid").asText("");
                                jp.name      = property.path("name").asText("文档任务");
                                jobs.add(jp);
                                break;

                            case "workid":
                                jp.type  = "workid";
                                jp.jobId = card.path("jobid").asText("");
                                jp.name  = property.path("name").asText("作业任务");
                                if (StringUtils.hasText(jp.jobId)) jobs.add(jp);
                                break;

                            default:
                                log.debug("未知任务类型：{}", cardType);
                        }
                    }

                sleepRandom(400, 700);
            }
            if (!hasAnyCards) {
                studyEmptyPage(client, task, point, cpi, taskLogger);
            }

        } catch (Exception e) {
            log.error("提取任务点异常：", e);
        }
        return new JobFetchResult(jobs, jobInfo, hasAnyCards);
    }

    private void studyEmptyPage(OkHttpClient client, CourseTask task,
                                KnowledgePoint point, String cpi, TaskLogger taskLogger) {
        try {
            HttpUrl url = Objects.requireNonNull(
                            HttpUrl.parse("https://mooc1.chaoxing.com/mooc-ans/mycourse/studentstudyAjax")).newBuilder()
                    .addQueryParameter("courseId",      task.getCourseId())
                    .addQueryParameter("clazzid",       task.getClassId())
                    .addQueryParameter("chapterId",     point.id)
                    .addQueryParameter("cpi",           cpi)
                    .addQueryParameter("verificationcode", "")
                    .addQueryParameter("mooc2",         "1")
                    .addQueryParameter("microTopicId",  "0")
                    .addQueryParameter("editorPreview", "0")
                    .build();

            Request req = new Request.Builder().url(url).header("User-Agent", USER_AGENT).get().build();
            String respStr = executeStringWithCaptchaCheck(client, req, taskLogger, task.getId());
            if (respStr != null) {
                taskLogger.log(task.getId(), "空章节上报完成：" + point.title);
            }
        } catch (Exception e) {
            log.warn("空章节上报异常：{}", e.getMessage());
        }
    }

    // ─────────────────────────────── 题目解析 ────────────────────────────── //
    private WorkQuestions parseWorkHtml(String html) {
        try {
            Document doc = Jsoup.parse(html);
            Element form = doc.selectFirst("form");
            if (form == null) return null;

            // 提取表单隐藏字段
            Map<String, String> formData = new LinkedHashMap<>();
            for (Element input : form.select("input")) {
                String name = input.attr("name");
                if (!StringUtils.hasText(name) || name.contains("answer")) continue;
                formData.put(name, input.attr("value"));
            }

            // 解析题目
            List<WorkQuestion> questions = new ArrayList<>();
            for (Element qDiv : form.select("div.singleQuesId")) {
                WorkQuestion q = new WorkQuestion();
                q.id = qDiv.attr("data");

                Element timu = qDiv.selectFirst("div.TiMu");
                if (timu == null) continue;
                String typeCode = timu.attr("data");
                q.type = resolveQuestionType(typeCode);
                Element titleDiv = qDiv.selectFirst("div.Zy_TItle");
                q.title = (titleDiv != null) ? titleDiv.text().trim() : "";

                Elements liItems = qDiv.select("ul li");
                StringBuilder optSb = new StringBuilder();
                for (Element li : liItems) {
                    optSb.append(li.text().trim()).append("\n");
                }
                q.options = new ArrayList<>(Arrays.asList(optSb.toString().trim().split("\n")));
                q.options.removeIf(s -> !StringUtils.hasText(s));

                Element answerTypeInput = qDiv.selectFirst("input[name^=answertype]");
                String answertypeKey = "answertype" + q.id;
                String answertypeVal = (answerTypeInput != null) ? answerTypeInput.attr("value") : "";
                q.rawAnswerType = answertypeVal;
                formData.put(answertypeKey, answertypeVal);

                Element tkSizeInput = qDiv.selectFirst("input[name=tiankongsize" + q.id + "]");
                if (tkSizeInput != null && StringUtils.hasText(tkSizeInput.attr("value"))) {
                    try {
                        q.completionCount = Integer.parseInt(tkSizeInput.attr("value"));
                    } catch (Exception ignored) {}
                } else {
                    int editors = qDiv.select("textarea, div.mce-edit-area, input.tiankong").size();
                    if (editors > 0) q.completionCount = editors;
                }

                if (!"2".equals(answertypeVal)) {
                    formData.put("answer" + q.id, "");
                }

                questions.add(q);
            }

            if (!questions.isEmpty()) {
                StringBuilder answerwqbid = new StringBuilder();
                for (WorkQuestion q : questions) {
                    answerwqbid.append(q.id).append(",");
                }
                formData.put("answerwqbid", answerwqbid.toString());
            }

            WorkQuestions result = new WorkQuestions();
            result.formData  = formData;
            result.questions = questions;
            return result;
        } catch (Exception e) {
            log.error("解析作业 HTML 异常：", e);
            return null;
        }
    }
    private String randomAnswer(WorkQuestion q) {
        if ("2".equals(q.rawAnswerType) || "completion".equals(q.type)) {
            return "略";
        }
        if ("4".equals(q.rawAnswerType) || "subjective".equals(q.type)) {
            return "正确";
        }
        if (q.options == null || q.options.isEmpty()) return "";
        switch (q.type) {
            case "single":
                return q.options.get(random.nextInt(q.options.size())).substring(0, 1);
            case "multiple":
                int count = 2 + random.nextInt(Math.max(1, Math.min(2, q.options.size() - 1)));
                List<String> shuffled = new ArrayList<>(q.options);
                Collections.shuffle(shuffled, random);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < Math.min(count, shuffled.size()); i++)
                    sb.append(shuffled.get(i).substring(0, 1));
                char[] chars = sb.toString().toCharArray();
                Arrays.sort(chars);
                return new String(chars);
            case "judgement":
                return random.nextBoolean() ? "true" : "false";
            default:
                return "";
        }
    }

    private String resolveQuestionType(String code) {
        switch (code) {
            case "0": return "single";
            case "1": return "multiple";
            case "2": return "completion";
            case "3": return "judgement";
            case "4": return "subjective";
            default:  return "other";
        }
    }

    private String[] splitCompletionAnswer(String answer, int count) {
        if (!StringUtils.hasText(answer)) {
            return new String[0];
        }
        if (count <= 1) {
            return new String[]{answer.trim()};
        }

        String[] parts = answer.split("[\n#|;；]|---");
        List<String> cleanParts = cleanAndFilter(parts);
        if (cleanParts.size() >= count) {
            return cleanParts.toArray(new String[0]);
        }
        parts = answer.split("[\n#|;；,，/\\\\、\\s]+|---");
        cleanParts = cleanAndFilter(parts);
        if (!cleanParts.isEmpty()) {
            return cleanParts.toArray(new String[0]);
        }

        return new String[]{answer.trim()};
    }

    private List<String> cleanAndFilter(String[] parts) {
        List<String> list = new ArrayList<>();
        for (String p : parts) {
            if (p != null && StringUtils.hasText(p.trim())) {
                list.add(p.trim());
            }
        }
        return list;
    }

    private String normalizeJudgementAnswer(String answer) {
        if (!StringUtils.hasText(answer)) return "true";
        String s = answer.trim().toLowerCase();
        if (s.contains("a") || s.contains("对") || s.contains("正确") || s.contains("true") || s.contains("t") || "1".equals(s)) {
            return "true";
        }
        if (s.contains("b") || s.contains("错") || s.contains("错误") || s.contains("false") || s.contains("f") || "0".equals(s)) {
            return "false";
        }
        return "true";
    }

    private String normalizeMultipleAnswer(String answer) {
        if (!StringUtils.hasText(answer)) return "A";
        String clean = answer.replaceAll("[^A-Za-z]", "").toUpperCase();
        if (!StringUtils.hasText(clean)) return "A";
        char[] chars = clean.toCharArray();
        Arrays.sort(chars);
        StringBuilder sb = new StringBuilder();
        for (char c : chars) {
            if (sb.indexOf(String.valueOf(c)) < 0) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String normalizeSingleAnswer(String answer) {
        if (!StringUtils.hasText(answer)) return "A";
        String clean = answer.replaceAll("[^A-Za-z]", "").toUpperCase();
        if (StringUtils.hasText(clean)) {
            return clean.substring(0, 1);
        }
        return "A";
    }

    // ─────────────────────────────── 工具方法 ────────────────────────────── //

    private OkHttpClient buildClient() {
        return OkHttpUtil.createUnsafeClientBuilder()
                .cookieJar(OkHttpUtil.createInMemoryCookieJar())
                .build();
    }

    private String aesEncrypt(String data, String key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(keyBytes, "AES"),
                new IvParameterSpec(keyBytes));
        return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    private String getCookieValue(OkHttpClient client, String domainUrl, String cookieName) {
        HttpUrl httpUrl = HttpUrl.parse(domainUrl);
        if (httpUrl == null || client.cookieJar() == null) return "";
        for (Cookie c : client.cookieJar().loadForRequest(httpUrl)) {
            if (c.name().equalsIgnoreCase(cookieName)) return c.value();
        }
        return "";
    }

    private String extractUrlParam(String url, String param) {
        if (!StringUtils.hasText(url)) return "";
        int start = url.indexOf(param + "=");
        if (start < 0) return "";
        start += param.length() + 1;
        int end = url.indexOf("&", start);
        return end < 0 ? url.substring(start) : url.substring(start, end);
    }

    private String extractNodeId(String otherInfo) {
        if (!StringUtils.hasText(otherInfo)) return "";
        Matcher m = Pattern.compile("nodeId_(.*?)-").matcher(otherInfo);
        return m.find() ? m.group(1) : "";
    }

    private void sleepRandom(int minMs, int maxMs) {
        try {
            Thread.sleep(minMs + random.nextInt(maxMs - minMs));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "…";
    }

    // ─────────────────────────────── 验证码自动处理引擎 ────────────────────────────── //

    /**
     * 检测响应文本是否包含学习通验证码触发特征
     */
    private boolean checkNeedCaptcha(String html) {
        if (!StringUtils.hasText(html)) return false;
        return html.contains("processVerify.ac") || html.contains("【9010】") || html.contains("操作异常，请输入图片中的验证码");
    }

    /**
     * 统一网络请求包装：自动识别验证码并过验证码重试
     */
    private String executeStringWithCaptchaCheck(OkHttpClient client, Request req, TaskLogger taskLogger, Long taskId) throws Exception {
        for (int retry = 0; retry < 3; retry++) {
            generalRateLimiter.limitRate(true, 100, 300);
            try (Response resp = client.newCall(req).execute()) {

                if (!resp.isSuccessful() || resp.body() == null) {
                    return null;
                }
                String bodyStr = resp.body().string();
                if (checkNeedCaptcha(bodyStr)) {
                    boolean solved = handleCaptchaPass(client, taskLogger, taskId);
                    if (solved) {
                        sleepRandom(1000, 2000);
                        continue; // 破解成功后自动重试原请求
                    }
                }
                return bodyStr;
            }
        }
        return null;
    }

    /**
     * 尝试通过验证码自动解锁
     */
    private boolean handleCaptchaPass(OkHttpClient client, TaskLogger taskLogger, Long taskId) {
        if (taskLogger != null && taskId != null) {
            taskLogger.log(taskId, "检测到超星安全验证码【9010】，正在调用 CaptchaDecoder 自动解码过验证...");
        }
        log.info("检测到超星安全验证码，正在调用 CaptchaDecoder 自动过验证...");

        // 方案 1：Java 客户端自带 Cookie 下载验证码图片 -> 发送 OCR 识别 -> Java 提交验证
        try {
            long randomT = (long) (Math.random() * 2147483647);
            String getImgUrl = "https://mooc1.chaoxing.com/processVerifyPng.ac?t=" + randomT;
            Request imgReq = new Request.Builder()
                    .url(getImgUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                    .get().build();

            byte[] imgBytes = null;
            try (Response imgResp = client.newCall(imgReq).execute()) {
                if (imgResp.isSuccessful() && imgResp.body() != null) {
                    imgBytes = imgResp.body().bytes();
                }
            }

            if (imgBytes != null && imgBytes.length > 0) {
                String ocrUrl = captchaDecoderUrl + "/ocr";
                RequestBody rawBody = RequestBody.create(imgBytes, MediaType.parse("image/png"));
                Request ocrReq = new Request.Builder()
                        .url(ocrUrl)
                        .post(rawBody)
                        .build();

                String capCode = null;
                try (Response ocrResp = client.newCall(ocrReq).execute()) {
                    if (ocrResp.isSuccessful() && ocrResp.body() != null) {
                        JsonNode json = objectMapper.readTree(ocrResp.body().string());
                        if (json.path("code").asInt(-1) == 0) {
                            capCode = json.path("result").asText("");
                        }
                    }
                }

                if (StringUtils.hasText(capCode)) {
                    if (taskLogger != null && taskId != null) {
                        taskLogger.log(taskId, "[验证码] OCR识别结果为: " + capCode + "，正在提交解锁...");
                    }
                    String submitUrl = "https://mooc1.chaoxing.com/html/processVerify.ac?ucode=" + capCode + "&app=0";
                    Request submitReq = new Request.Builder()
                            .url(submitUrl)
                            .header("User-Agent", USER_AGENT)
                            .get().build();

                    try (Response submitResp = client.newCall(submitReq).execute()) {
                        if (submitResp.code() == 302 || submitResp.code() == 200) {
                            if (taskLogger != null && taskId != null) {
                                taskLogger.log(taskId, "验证码通过成功！已恢复任务执行。");
                            }
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("方案1本地处理验证码失败，尝试方案2代理全处理: {}", e.getMessage());
        }

        // 方案 2：如果方案 1 未成功，尝试将当前系统的 Cookie 提取传给 CaptchaDecoder 的 /solve 接口
        try {
            HttpUrl moocUrl = HttpUrl.parse("https://mooc1.chaoxing.com");
            StringBuilder cookieSb = new StringBuilder();
            if (moocUrl != null && client.cookieJar() != null) {
                List<Cookie> cookies = client.cookieJar().loadForRequest(moocUrl);
                for (int i = 0; i < cookies.size(); i++) {
                    if (i > 0) cookieSb.append("; ");
                    cookieSb.append(cookies.get(i).name()).append("=").append(cookies.get(i).value());
                }
            }

            Map<String, String> payload = new HashMap<>();
            payload.put("platform", "chaoxing");
            payload.put("type", "ddddocr");
            payload.put("cookies", cookieSb.toString());
            payload.put("user_agent", USER_AGENT);


            RequestBody jsonBody = RequestBody.create(
                    objectMapper.writeValueAsString(payload),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request solveReq = new Request.Builder()
                    .url(captchaDecoderUrl + "/solve")
                    .post(jsonBody)
                    .build();

            try (Response solveResp = client.newCall(solveReq).execute()) {
                if (solveResp.isSuccessful() && solveResp.body() != null) {
                    JsonNode json = objectMapper.readTree(solveResp.body().string());
                    boolean success = json.path("success").asBoolean(false);
                    if (success) {
                        if (taskLogger != null && taskId != null) {
                            taskLogger.log(taskId, "验证码通过成功（后端代理服务解封）！已恢复任务执行。");
                        }
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.error("调用 CaptchaDecoder 验证码服务全面失败: {}", e.getMessage());
        }

        if (taskLogger != null && taskId != null) {
            taskLogger.log(taskId, "验证码自动过验证失败，请稍后重试或手动处理。");
        }
        return false;
    }

    // ─────────────────────────────── 内部数据类 ────────────────────────────── //


    private static class KnowledgePoint {
        String id;
        String title;
    }

    private static class JobPoint {
        String jobId   = "";
        String objectId = "";
        String otherInfo = "";
        String name    = "";
        String type    = "";
        String knowledgeId = "";
        String jtoken  = "";
        String mid     = "";
        String enc     = "";
        String aid     = "";
        String rt      = "";
        String attDuration = "";
        String attDurationEnc = "";
        String videoFaceCaptureEnc = "";
        int    playTime = 0;
    }

    private static class JobInfo {
        String ktoken      = "";
        String mtEnc       = "";
        String defenc      = "";
        String cardid      = "";
        String cpi         = "";
        String qnenc       = "";
        String knowledgeid = "";
        int    reportTimeInterval = 60;
    }

    private static class JobFetchResult {
        final List<JobPoint> jobs;
        final JobInfo        jobInfo;
        final boolean        hasAnyCards;
        JobFetchResult(List<JobPoint> jobs, JobInfo jobInfo, boolean hasAnyCards) {
            this.jobs        = jobs;
            this.jobInfo     = jobInfo;
            this.hasAnyCards = hasAnyCards;
        }
    }

    private static class VideoLogResult {
        final boolean isPassed;
        final int     statusCode;
        VideoLogResult(boolean isPassed, int statusCode) {
            this.isPassed   = isPassed;
            this.statusCode = statusCode;
        }
    }

    private static class WorkQuestion {
        String id;
        String type;
        String rawAnswerType;
        String title;
        List<String> options;
        int completionCount = 1;
    }

    private static class WorkQuestions {
        Map<String, String>  formData;
        List<WorkQuestion>   questions;
    }

    private static class RateLimiter {
        private final long callIntervalMs;
        private long lastCallTime;
        private final Object lock = new Object();

        public RateLimiter(long callIntervalMs) {
            this.callIntervalMs = callIntervalMs;
            this.lastCallTime = System.currentTimeMillis();
        }

        public void limitRate(boolean randomTime, long randomMinMs, long randomMaxMs) {
            long waitTime = 0;
            synchronized (lock) {
                long now = System.currentTimeMillis();
                long baseWait = Math.max(lastCallTime + callIntervalMs - now, 0);
                long extraWait = 0;
                if (randomTime && randomMaxMs > randomMinMs) {
                    extraWait = randomMinMs + (long) (Math.random() * (randomMaxMs - randomMinMs));
                }
                waitTime = baseWait + extraWait;
                lastCallTime = now + waitTime;
            }
            if (waitTime > 0) {
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}