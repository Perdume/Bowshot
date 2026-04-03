package bowshot.debug;

/**
 * 모든 에러/진단 코드.
 * 형식: BS-XYYY (X=모듈, YYY=번호)
 *
 * 1xxx = 초기화/설정
 * 2xxx = 아레나/월드
 * 3xxx = 게임 라이프사이클
 * 4xxx = 매칭
 * 5xxx = 점수/ELO
 * 6xxx = 안티치트
 * 7xxx = 리플레이
 * 8xxx = 명령어
 */
public enum ErrorCode {

    // === 1xxx: Init / Config ===
    INIT_CONFIG_LOAD_FAIL("BS-1001", Severity.ERROR, "설정 파일 로드 실패"),
    INIT_LANG_LOAD_FAIL("BS-1002", Severity.ERROR, "언어 파일 로드 실패"),
    INIT_DATA_FILE_FAIL("BS-1003", Severity.ERROR, "데이터 파일 로드 실패"),
    INIT_COMMAND_REGISTER_FAIL("BS-1004", Severity.ERROR, "명령어 등록 실패"),
    INIT_ARENA_LOAD_FAIL("BS-1005", Severity.WARN, "아레나 목록 로드 실패"),

    // === 2xxx: Arena / World ===
    ARENA_NOT_FOUND("BS-2001", Severity.WARN, "아레나를 찾을 수 없음"),
    ARENA_NO_AVAILABLE("BS-2002", Severity.ERROR, "사용 가능한 아레나 없음"),
    ARENA_ALREADY_EXISTS("BS-2003", Severity.WARN, "이미 존재하는 아레나"),
    WORLD_TEMPLATE_MISSING("BS-2004", Severity.ERROR, "월드 템플릿 파일 없음"),
    WORLD_CREATE_FAIL("BS-2005", Severity.ERROR, "인스턴스 월드 생성 실패"),
    WORLD_REMOVE_FAIL("BS-2006", Severity.WARN, "인스턴스 월드 삭제 실패"),
    WORLD_UNLOAD_FAIL("BS-2007", Severity.WARN, "월드 언로드 실패"),
    WORLD_SPAWN_NOT_SET("BS-2008", Severity.WARN, "스폰 위치 미설정"),
    WORLD_LOBBY_NOT_SET("BS-2009", Severity.WARN, "로비 위치 미설정"),
    WORLD_FILE_NOT_FOUND("BS-2010", Severity.ERROR, "월드 파일 없음"),
    WORLD_ORPHANED("BS-2011", Severity.WARN, "고아 인스턴스 월드 발견"),

    // === 3xxx: Game Lifecycle ===
    GAME_START_NO_ARENA("BS-3001", Severity.ERROR, "게임 시작 실패 - 아레나 없음"),
    GAME_START_NO_PLAYERS("BS-3002", Severity.WARN, "게임 시작 실패 - 플레이어 없음"),
    GAME_START_FAIL("BS-3003", Severity.ERROR, "게임 시작 중 오류 발생"),
    GAME_END_ALREADY("BS-3004", Severity.WARN, "이미 종료된 게임"),
    GAME_END_FAIL("BS-3005", Severity.ERROR, "게임 종료 중 오류 발생"),
    GAME_TIMEOUT("BS-3006", Severity.INFO, "게임 시간 초과로 종료"),
    GAME_PLAYER_DISCONNECT("BS-3007", Severity.INFO, "게임 중 플레이어 연결 끊김"),
    GAME_TASK_LEAK("BS-3008", Severity.WARN, "게임 태스크 미정리 감지"),
    GAME_PLAYER_NULL("BS-3009", Severity.ERROR, "게임 플레이어 null 참조"),
    GAME_ZERO_KILLS("BS-3010", Severity.INFO, "전체 0킬 게임 (존버 의심)"),
    GAME_ONE_SIDED("BS-3011", Severity.INFO, "일방적 게임 감지"),

    // === 4xxx: Matchmaking ===
    MATCH_QUEUE_FULL("BS-4001", Severity.WARN, "매칭 대기열 초과"),
    MATCH_NO_OPPONENTS("BS-4002", Severity.INFO, "매칭 상대 없음"),
    MATCH_MMR_GAP("BS-4003", Severity.WARN, "매칭된 플레이어 간 MMR 격차 과대"),
    MATCH_TIMEOUT_FALLBACK("BS-4004", Severity.INFO, "대기 시간 초과로 폴백 매칭"),
    MATCH_PLAYER_OFFLINE("BS-4005", Severity.WARN, "매칭 중 플레이어 오프라인"),
    MATCH_START_FAIL("BS-4006", Severity.ERROR, "매칭 후 게임 시작 실패"),
    MATCH_LONG_WAIT("BS-4007", Severity.INFO, "장시간 매칭 대기"),

    // === 5xxx: Score / MMR ===
    SCORE_SAVE_FAIL("BS-5001", Severity.ERROR, "점수 저장 실패"),
    SCORE_LOAD_FAIL("BS-5002", Severity.ERROR, "점수 로드 실패"),
    SCORE_NEGATIVE_RESULT("BS-5003", Severity.WARN, "MMR 음수 보정 발생"),
    SCORE_EXTREME_GAP("BS-5004", Severity.WARN, "플레이어 간 MMR 극심한 격차"),
    SCORE_INFLATION("BS-5005", Severity.INFO, "전체 MMR 인플레이션 의심"),
    SCORE_DEFLATION("BS-5006", Severity.INFO, "전체 MMR 디플레이션 의심"),

    // === 6xxx: AntiCheat ===
    AC_REACH_DETECT("BS-6001", Severity.INFO, "리치 핵 감지"),
    AC_SPEED_DETECT("BS-6002", Severity.INFO, "스피드 핵 감지"),
    AC_CPS_DETECT("BS-6003", Severity.INFO, "오토클릭 감지"),
    AC_PLAYER_REMOVED("BS-6004", Severity.WARN, "안티치트에 의한 플레이어 제거"),
    AC_FALSE_POSITIVE("BS-6005", Severity.INFO, "오탐 가능성 있는 탐지"),

    // === 7xxx: Replay ===
    REPLAY_SAVE_FAIL("BS-7001", Severity.ERROR, "리플레이 저장 실패"),
    REPLAY_LOAD_FAIL("BS-7002", Severity.ERROR, "리플레이 로드 실패"),
    REPLAY_NOT_FOUND("BS-7003", Severity.WARN, "리플레이를 찾을 수 없음"),
    REPLAY_RECORD_FAIL("BS-7004", Severity.ERROR, "리플레이 녹화 중 오류"),
    REPLAY_MAX_EXCEEDED("BS-7005", Severity.WARN, "최대 리플레이 수 초과"),

    // === 8xxx: Command ===
    CMD_NO_PERMISSION("BS-8001", Severity.INFO, "권한 없는 명령어 시도"),
    CMD_INVALID_ARGS("BS-8002", Severity.INFO, "잘못된 명령어 인수"),
    CMD_PLAYER_ONLY("BS-8003", Severity.INFO, "플레이어 전용 명령어"),
    CMD_EXECUTION_FAIL("BS-8004", Severity.ERROR, "명령어 실행 중 오류");

    private final String code;
    private final Severity severity;
    private final String description;

    ErrorCode(String code, Severity severity, String description) {
        this.code = code;
        this.severity = severity;
        this.description = description;
    }

    public String getCode() { return code; }
    public Severity getSeverity() { return severity; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return "[" + code + "] " + description;
    }

    public String format(Object... args) {
        String msg = description;
        for (int i = 0; i < args.length; i++) {
            msg = msg + " (" + args[i] + ")";
        }
        return "[" + code + "] " + msg;
    }

    public enum Severity {
        INFO, WARN, ERROR
    }
}
