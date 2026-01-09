#!/bin/zsh
# setup.sh
echo "⚙️  프로젝트 환경 설정을 시작합니다..."

# 1. 훅 복사 및 권한 부여
mkdir -p .git/hooks
cp scripts/hooks/* .git/hooks/
chmod +x .git/hooks/*

# 2. 제미나이 CLI 설치 확인 (팀원도 CLI가 있어야 함)
if ! command -v gemini &> /dev/null; then
    echo "⚠️  Gemini CLI가 설치되어 있지 않습니다. 설치 가이드를 확인하세요."
else
    echo "✅ Gemini CLI 확인 완료."
fi

echo "🚀 설정이 완료되었습니다."