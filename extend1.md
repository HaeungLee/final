# 프로젝트 확장 계획 (extend1.md)

## 🎯 현재 상태 (MVP 완성)
- ✅ React 19 + Node.js + Spring Security + FastAPI STT/TTS 통합 완료
- ✅ OAuth2 소셜 로그인 시스템 구축
- ✅ 음성 인식/합성 기본 기능 동작
- 🔄 **현재 진행 중**: UI/UX 개선 (팀원 담당)

## 📱 Phase 1: React Native 모바일 확장

### 1.1 기술적 실현 가능성
- **난이도**: ⭐⭐⭐ (중간)
- **예상 기간**: 1-2주
- **코드 재사용률**: 약 80% (로직, API 인터페이스, 타입 정의)

### 1.2 주요 변경사항

#### 네트워크 계층
```typescript
// 현재 (웹)
const response = await fetch('/api/stt/start')

// 변경 후 (모바일)
const response = await fetch('http://your-server:8082/api/stt/start')
```

#### 음성 처리
```typescript
// 현재 (웹): MediaRecorder API
navigator.mediaDevices.getUserMedia()

// 변경 후 (모바일): React Native 라이브러리
import { AudioRecorder } from 'react-native-audio-recorder-player'
```

### 1.3 필요한 라이브러리
```json
{
  "dependencies": {
    "react-native": "^0.73.x",
    "react-native-audio-recorder-player": "^3.5.x",
    "react-native-fs": "^2.20.x",
    "@react-native-async-storage/async-storage": "^1.21.x",
    "react-native-keychain": "^8.1.x"
  }
}
```

### 1.4 구현 단계
1. **환경 설정** (1일)
   - React Native CLI 설치
   - Android/iOS 개발 환경 구축
   
2. **컴포넌트 마이그레이션** (3-4일)
   - 기존 React 컴포넌트를 React Native로 변환
   - 네비게이션 시스템 구축 (React Navigation)
   
3. **음성 기능 통합** (2-3일)
   - 네이티브 오디오 녹음/재생 구현
   - 파일 업로드/다운로드 최적화
   
4. **테스트 및 디버깅** (2-3일)
   - 실기기 테스트
   - 성능 최적화

### 1.5 모바일 특화 기능 추가 가능
- 🔔 푸시 알림 (음성 처리 완료 시)
- 📱 백그라운드 처리 (음성 녹음 중 앱 전환)
- 🎙️ 더 나은 마이크 권한 관리
- 💾 오프라인 모드 (임시 저장)

## 🎤 Phase 2: 실시간 음성 감지 (VAD) 개선

### 2.1 현재 문제점
- 15초 고정 녹음 → 사용자가 말을 끝내도 대기
- 사용자 경험 저하

### 2.2 해결 방안

#### 방안 A: 클라이언트 측 VAD (1순위 - 빠른 구현)
```javascript
// Web Audio API 실시간 볼륨 분석
const detectSilence = () => {
    const volume = getAudioLevel()
    if (volume < THRESHOLD && silenceDuration > 2000) {
        stopRecording() // 2초 무음 시 자동 종료
    }
}
```

**장점**: 
- 즉시 반응 (지연 없음)
- 서버 부하 없음
- 구현 간단

**단점**:
- 환경 소음에 민감
- 임계값 조정 필요

#### 방안 B: 서버 측 WebSocket VAD (2순위 - 고도화)
```python
# FastAPI + WebSocket 실시간 스트리밍
import webrtcvad

async def stream_vad():
    vad = webrtcvad.Vad(2)
    async for chunk in websocket:
        if silence_detected(chunk):
            await finalize_stt()
```

**장점**:
- 더 정확한 음성 감지
- AI 기반 판단

**단점**:
- WebSocket 연결 복잡도 증가
- 서버 자원 사용량 증가

### 2.3 구현 우선순위
1. **1단계**: 클라이언트 VAD (1-2일) ⭐⭐⭐
2. **2단계**: 사용자 피드백 수집 (1주)
3. **3단계**: 필요시 서버 VAD 고도화 (1-2주)

## 🚀 Phase 3: 고급 기능 확장

### 3.1 AI 기능 강화
- 🌍 **다국어 지원**: Whisper 다국어 모델 활용
- 😊 **감정 인식**: 음성 톤 분석
- 🎭 **화자 분리**: 여러 명이 말할 때 구분

### 3.2 TTS 음성 품질 향상
- 🎪 **다양한 음성**: ElevenLabs 프리미엄 음성
- ⚡ **실시간 TTS**: 스트리밍 기반 음성 합성
- 🎨 **감정 표현**: 텍스트에 따른 감정 조절

### 3.3 시스템 최적화
- 📊 **모니터링**: Prometheus + Grafana
- 🔄 **로드밸런싱**: nginx 기반 서버 분산
- 💾 **캐싱**: Redis 기반 응답 캐시

## 📋 실행 계획

### 즉시 시작 가능 (UI 완성 대기 중)
1. React Native 개발 환경 준비
2. 기존 컴포넌트 분석 및 마이그레이션 계획 수립
3. 클라이언트 VAD 프로토타입 개발

### UI 완성 후 본격 시작
1. **Week 1-2**: React Native 앱 기본 구조 구축
2. **Week 3**: 음성 기능 통합 및 테스트
3. **Week 4**: VAD 기능 추가 및 UX 개선

## 🎯 성공 지표

### Phase 1 완성 시
- [ ] iOS/Android 앱에서 로그인 가능
- [ ] 모바일에서 음성 녹음/재생 동작
- [ ] 웹과 동일한 기능 제공

### Phase 2 완성 시
- [ ] 평균 대기시간 50% 단축 (15초 → 7.5초)
- [ ] 사용자 만족도 향상
- [ ] 무음 구간 자동 감지 정확도 90% 이상

## 📚 참고 자료
- [React Native 공식 문서](https://reactnative.dev/)
- [WebRTC VAD 라이브러리](https://github.com/wiseman/py-webrtcvad)
- [Whisper 실시간 처리](https://github.com/collabora/WhisperLive)

---

**📝 Note**: UI 팀원 작업 완료 후 Phase 1부터 순차적으로 진행 예정 