#!/usr/bin/env bash

set -e

echo "============================================================"
echo " RahulSMGV CBS - Transaction Service Inspection"
echo "============================================================"

echo
echo ">>> Current directory"
pwd

echo
echo ">>> Git status"
git status --short 2>/dev/null || true

echo
echo "============================================================"
echo "1. AccountService port"
echo "============================================================"

find src/main/java -type f -name "AccountService.java" -print -exec sed -n '1,240p' {} \;

echo
echo "============================================================"
echo "2. CreateTransactionRequest"
echo "============================================================"

find src/main/java -type f -name "CreateTransactionRequest.java" -print -exec sed -n '1,260p' {} \;

echo
echo "============================================================"
echo "3. Transaction controllers"
echo "============================================================"

find src/main/java -type f \
    \( -name "*Controller.java" -o -name "*ControllerAdvice.java" \) \
    -print \
    -exec sh -c 'echo "----- $1 -----"; sed -n "1,320p" "$1"' _ {} \;

echo
echo "============================================================"
echo "4. Application DTOs"
echo "============================================================"

find src/main/java/com/rahulsmgv/cbs/transaction/application/dto \
    -type f -name "*.java" \
    -print \
    -exec sh -c 'echo "----- $1 -----"; sed -n "1,320p" "$1"' _ {} \; 2>/dev/null || true

echo
echo "============================================================"
echo "5. Application ports"
echo "============================================================"

find src/main/java/com/rahulsmgv/cbs/transaction/application/port \
    -type f -name "*.java" \
    -print \
    -exec sh -c 'echo "----- $1 -----"; sed -n "1,260p" "$1"' _ {} \; 2>/dev/null || true

echo
echo "============================================================"
echo "6. TransactionApplicationService"
echo "============================================================"

find src/main/java -type f -name "TransactionApplicationService.java" \
    -print \
    -exec sed -n '1,360p' {} \;

echo
echo "============================================================"
echo "7. Transaction domain model"
echo "============================================================"

find src/main/java -type f \
    \( -name "Transaction.java" -o -name "Money.java" \) \
    -print \
    -exec sh -c 'echo "----- $1 -----"; sed -n "1,360p" "$1"' _ {} \;

echo
echo "============================================================"
echo "8. Transaction persistence"
echo "============================================================"

find src/main/java -type f \
    \( -name "*TransactionEntity.java" \
    -o -name "*TransactionRepository.java" \
    -o -name "*TransactionRepositoryAdapter.java" \) \
    -print \
    -exec sh -c 'echo "----- $1 -----"; sed -n "1,360p" "$1"' _ {} \;

echo
echo "============================================================"
echo "9. pom.xml"
echo "============================================================"

sed -n '1,360p' pom.xml

echo
echo "============================================================"
echo "10. Configuration files"
echo "============================================================"

find src/main/resources src/test/resources \
    -type f \
    \( -name "*.yml" -o -name "*.yaml" -o -name "*.properties" \) \
    -print \
    -exec sh -c 'echo "----- $1 -----"; sed -n "1,360p" "$1"' _ {} \; 2>/dev/null || true

echo
echo "============================================================"
echo "11. Existing AccountService references"
echo "============================================================"

grep -Rni \
    --exclude-dir=target \
    --exclude-dir=.git \
    -E "AccountService|accountService|account-service|account.service" \
    src/main src/test 2>/dev/null || true

echo
echo "============================================================"
echo "12. HTTP / REST client dependencies"
echo "============================================================"

grep -n -E \
    "webflux|webmvc|restclient|openfeign|feign|httpclient|okhttp|webclient|rest" \
    pom.xml 2>/dev/null || true

echo
echo "============================================================"
echo "13. Existing tests"
echo "============================================================"

find src/test -type f -name "*.java" \
    -print \
    -exec sh -c 'echo "----- $1 -----"; sed -n "1,420p" "$1"' _ {} \; 2>/dev/null || true

echo
echo "============================================================"
echo " Inspection complete"
echo "============================================================"
