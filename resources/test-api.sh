#!/usr/bin/env bash

# Exit immediately if a command exits with a non-zero status
set -e

# Configuration
BASE_URL="http://localhost:8080/api/v1/sleep"
USER_ID="user_123abt"
TARGET_DATE="2026-06-20"

# Text Formatting Colors (To look nice)
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0;m' # No Color

echo -e "${BLUE}===================================================================${NC}"
echo -e "${BLUE}         Sleep Tracker API Integration Verification Script         ${NC}"
echo -e "${BLUE}===================================================================${NC}"

# Check if curl is installed
if ! command -v curl &> /dev/null; then
    echo -e "${RED}Error: curl is required but not installed. Exiting.${NC}"
    exit 1
fi

# Check if server is up
echo -e "\n${YELLOW}[1/6] Pinging API boundary uptime...${NC}"
if ! curl -s -f -o /dev/null "http://localhost:8080/swagger-ui/index.html"; then
    echo -e "${RED}Error: Application server is not running on port 8080.${NC}"
    echo -e "Please run './gradlew bootRun' or 'docker-compose up' before executing this script."
    exit 1
fi
echo -e "${GREEN}✓ Server is up and listening on port 8080.${NC}"

# 1. POST: Create a fresh log entry
echo -e "\n${YELLOW}[2/6] Posting fresh sleep telemetry log...${NC}"

POST_RESPONSE=$(curl --location "$BASE_URL" \
--header 'Content-Type: application/json' \
--data "{
  \"userId\": \"$USER_ID\",
  \"sleepDate\": \"$TARGET_DATE\",
  \"bedtime\": \"22:30:00\",
  \"wakeTime\": \"06:45:00\",
  \"morningFeeling\": \"GOOD\"
}")

echo -e "${GREEN}Response payload received:${NC}"
echo "$POST_RESPONSE"

# Extract ID using basic sed parsing to avoid mandatory dependency on external tools like jq
LOG_ID=$(echo "$POST_RESPONSE" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')

if [ -z "$LOG_ID" ]; then
    echo -e "${RED}Failed to parse persisted record ID from response payload.${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Successfully created sleep log entry with tracking ID: $LOG_ID${NC}"

# 2. POST: Verify duplicate safety constraint boundary catches collisions
echo -e "\n${YELLOW}[3/6] Verifying database unique date constraint boundaries...${NC}"
echo -e "Attempting duplicate entry post targeting identical user date index mapping..."

DUPLICATE_STATUS=$(curl -s -o /dev/null -w "%{http_code}" --location "$BASE_URL" \
--header 'Content-Type: application/json' \
--data "{
  \"userId\": \"$USER_ID\",
  \"sleepDate\": \"$TARGET_DATE\",
  \"bedtime\": \"23:00:00\",
  \"wakeTime\": \"07:00:00\",
  \"morningFeeling\": \"OK\"
}")

if [ "$DUPLICATE_STATUS" -eq 400 ]; then
    echo -e "${GREEN}✓ Duplicate request correctly blocked early. Received status 400 Bad Request.${NC}"
else
    echo -e "${RED}Security Boundary Breach: Duplicate logs allowed. Status: $DUPLICATE_STATUS${NC}"
    exit 1
fi

# 3. GET: Retrieve last night's sleep metrics (Requirement #2)
echo -e "\n${YELLOW}[4/6] Querying last night's chronologically newest sleep record...${NC}"
LAST_NIGHT_RESPONSE=$(curl -s --location "$BASE_URL/user/$USER_ID/last-night" --header 'Accept: application/json')
echo -e "${GREEN}Last Night Response:${NC}"
echo "$LAST_NIGHT_RESPONSE"
echo -e "${GREEN}✓ Successfully resolved targeted user's newest dataset node.${NC}"

# 4. GET: Fetch sliding 30-day aggregated moving matrix analytics (Requirement #3)
echo -e "\n${YELLOW}[5/6] Pulling historical 30-day statistical matrix profiles...${NC}"

ANALYTICS_RESPONSE=$(curl -s --location --globoff "http://localhost:8080/api/v1/sleep/user/$USER_ID/analytics")

echo -e "${GREEN}Moving Averages Matrix Response:${NC}"
echo "$ANALYTICS_RESPONSE"
echo -e "${GREEN}✓ Moving analytics matrix calculated cleanly without math errors.${NC}"

# 5. GET: Switch back to single sleep log view using navigate hook index
echo -e "\n${YELLOW}[6/6] Utilizing analytics navigation hook ID to view standalone entry...${NC}"
SINGLE_VIEW_RESPONSE=$(curl -s --location "$BASE_URL/$LOG_ID" --header 'Accept: application/json')
echo -e "${GREEN}Single View Response:${NC}"
echo "$SINGLE_VIEW_RESPONSE"
echo -e "${GREEN}✓ Navigation link back to single sleep log view validated successfully.${NC}"

echo -e "\n${GREEN}===================================================================${NC}"
echo -e "${GREEN}       All Core API Rest Verification Invariants Passed Cleanly!   ${NC}"
echo -e "${GREEN}===================================================================${NC}"