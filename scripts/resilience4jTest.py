import requests
import time
import json
from datetime import datetime
from concurrent.futures import ThreadPoolExecutor, as_completed

# 配置
GATEWAY_URL = "http://localhost:8080/api/v1/users/register"
TOTAL_REQUESTS = 20
MAX_WORKERS = 20  # 最大并发线程数

# 测试数据
test_user = {
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123"
}

def log_message(message, level="INFO"):
    """打印带时间戳的日志"""
    timestamp = datetime.now().strftime("%H:%M:%S.%f")[:-3]
    print(f"[{timestamp}] [{level}] {message}")

def send_single_request(request_num):
    """发送单个请求并记录结果"""
    log_message(f"🚀 启动请求 #{request_num}")

    start_time = time.time()

    try:
        # 🔧 关键修改:添加正确的请求头
        headers = {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        }

        response = requests.post(
            GATEWAY_URL,
            json=test_user,
            headers=headers,  # ✅ 添加这一行
            timeout=10
        )

        elapsed_time = time.time() - start_time

        # 解析响应
        try:
            response_data = response.json()
        except:
            response_data = response.text

        # 根据状态码判断结果
        if response.status_code == 200 or response.status_code == 201:
            log_message(
                f"✅ 请求 #{request_num} 成功 | "
                f"状态码: {response.status_code} | "
                f"耗时: {elapsed_time:.2f}s",
                "SUCCESS"
            )
        else:
            log_message(
                f"❌ 请求 #{request_num} 失败 | "
                f"状态码: {response.status_code} | "
                f"耗时: {elapsed_time:.2f}s | "
                f"响应: {json.dumps(response_data, ensure_ascii=False) if isinstance(response_data, dict) else response_data[:100]}",
                "ERROR"
            )

        return {
            "request_num": request_num,
            "status_code": response.status_code,
            "elapsed_time": elapsed_time,
            "success": response.status_code in [200, 201],
            "response": response_data
        }

    except requests.exceptions.Timeout:
        elapsed_time = time.time() - start_time
        log_message(
            f"⏱️ 请求 #{request_num} 超时 | 耗时: {elapsed_time:.2f}s",
            "TIMEOUT"
        )
        return {
            "request_num": request_num,
            "status_code": 0,
            "elapsed_time": elapsed_time,
            "success": False,
            "error": "Timeout"
        }

    except requests.exceptions.ConnectionError as e:
        elapsed_time = time.time() - start_time
        log_message(
            f"🔌 请求 #{request_num} 连接失败 | 耗时: {elapsed_time:.2f}s | 错误: {str(e)[:50]}",
            "CONNECTION_ERROR"
        )
        return {
            "request_num": request_num,
            "status_code": 0,
            "elapsed_time": elapsed_time,
            "success": False,
            "error": f"Connection Error: {str(e)[:50]}"
        }

    except Exception as e:
        elapsed_time = time.time() - start_time
        log_message(
            f"💥 请求 #{request_num} 发生异常 | 耗时: {elapsed_time:.2f}s | 错误: {str(e)[:50]}",
            "EXCEPTION"
        )
        return {
            "request_num": request_num,
            "status_code": 0,
            "elapsed_time": elapsed_time,
            "success": False,
            "error": str(e)[:50]
        }


def send_concurrent_requests(num_requests):
    """并发发送多个请求"""
    log_message(f"⚡ 准备并发发送 {num_requests} 个请求...", "INFO")

    results = []
    start_time = time.time()

    # 使用线程池并发执行
    with ThreadPoolExecutor(max_workers=MAX_WORKERS) as executor:
        # 提交所有任务
        futures = {executor.submit(send_single_request, i): i for i in range(1, num_requests + 1)}

        # 收集结果
        for future in as_completed(futures):
            try:
                result = future.result()
                results.append(result)
            except Exception as e:
                log_message(f"💥 任务执行失败: {str(e)}", "EXCEPTION")

    total_time = time.time() - start_time
    log_message(f"\n⏱️ 全部请求完成,总耗时: {total_time:.2f}s", "INFO")

    # 按请求编号排序
    results.sort(key=lambda x: x["request_num"])
    return results

def analyze_results(results):
    """分析测试结果"""
    log_message("\n" + "=" * 80, "INFO")
    log_message("📈 测试结果统计", "INFO")
    log_message("=" * 80, "INFO")

    success_count = sum(1 for r in results if r["success"])
    failure_count = len(results) - success_count

    # 计算耗时统计
    times = [r["elapsed_time"] for r in results]
    avg_time = sum(times) / len(times) if times else 0
    min_time = min(times) if times else 0
    max_time = max(times) if times else 0

    # 统计状态码
    status_codes = {}
    for r in results:
        code = r["status_code"]
        status_codes[code] = status_codes.get(code, 0) + 1

    log_message(f"总请求数: {len(results)}", "INFO")
    log_message(f"成功: {success_count}", "SUCCESS")
    log_message(f"失败: {failure_count}", "ERROR")
    log_message(f"平均耗时: {avg_time:.2f}s", "INFO")
    log_message(f"最短耗时: {min_time:.2f}s", "INFO")
    log_message(f"最长耗时: {max_time:.2f}s", "INFO")

    log_message("\n状态码分布:", "INFO")
    for code, count in sorted(status_codes.items()):
        if code == 0:
            log_message(f"  网络错误/超时: {count}", "ERROR")
        else:
            log_message(f"  {code}: {count}", "INFO")

    # 分析熔断器行为
    log_message("\n" + "=" * 80, "INFO")
    log_message("🔍 熔断器行为分析", "INFO")
    log_message("=" * 80, "INFO")

    # 找出快速失败的请求(耗时 < 1 秒)
    fast_failures = [r for r in results if not r["success"] and r["elapsed_time"] < 1.0]
    slow_failures = [r for r in results if not r["success"] and r["elapsed_time"] >= 1.0]

    log_message(f"慢速失败请求 (≥1s): {len(slow_failures)} 个", "INFO")
    log_message(f"快速失败请求 (<1s): {len(fast_failures)} 个", "INFO")

    if len(fast_failures) > 0:
        log_message(
            f"\n✅ 检测到 {len(fast_failures)} 个快速失败的请求!",
            "SUCCESS"
        )
        log_message(
            "这表明熔断器已打开,直接返回降级响应,无需等待超时。",
            "SUCCESS"
        )

        # 显示快速失败的请求编号
        fast_failure_nums = [r["request_num"] for r in fast_failures]
        log_message(f"快速失败的请求编号: {fast_failure_nums}", "INFO")
    else:
        log_message(
            "❌ 未检测到快速失败的请求,熔断器可能未正常工作!",
            "ERROR"
        )

    # 检查是否有 503 + 降级消息
    fallback_responses = [r for r in results if "Service is temporarily unavailable" in str(r.get("response", ""))]
    if fallback_responses:
        log_message(
            f"\n✅ 检测到 {len(fallback_responses)} 个降级响应!",
            "SUCCESS"
        )

def main():
    """主测试流程"""
    log_message("=" * 80, "INFO")
    log_message("🚀 开始测试 Gateway 熔断器功能 (并发模式)", "INFO")
    log_message("=" * 80, "INFO")
    log_message(f"目标 URL: {GATEWAY_URL}", "INFO")
    log_message(f"并发请求数: {TOTAL_REQUESTS}", "INFO")
    log_message(f"最大并发线程: {MAX_WORKERS}", "INFO")
    log_message("=" * 80, "INFO")
    log_message("⚠️ 请确保:", "INFO")
    log_message("  1. Consul 已启动", "INFO")
    log_message("  2. Gateway 已启动", "INFO")
    log_message("  3. User-Service 未启动 (模拟服务故障)", "INFO")
    log_message("=" * 80, "INFO")

    input("\n按回车键开始测试...")

    # 第一阶段:并发发送请求
    log_message("\n📊 第一阶段:并发发送 20 个请求 (User-Service 未启动)", "INFO")
    log_message("-" * 80, "INFO")

    results = send_concurrent_requests(TOTAL_REQUESTS)
    analyze_results(results)

    # 第二阶段:测试半开状态
    log_message("\n" + "=" * 80, "INFO")
    log_message("📊 第二阶段:测试半开状态", "INFO")
    log_message("=" * 80, "INFO")
    log_message("⏳ 等待 10 秒,让熔断器进入半开状态...", "INFO")

    for remaining in range(10, 0, -1):
        print(f"\r⏱️ 倒计时: {remaining} 秒...", end="", flush=True)
        time.sleep(1)
    print()

    log_message("\n⚠️ 现在请手动启动 user-service!", "NOTICE")
    input("启动完成后按回车继续...")

    log_message("\n发送测试请求,验证服务恢复...", "INFO")
    recovery_results = []
    for i in range(1, 6):
        result = send_single_request(f"恢复-{i}")
        recovery_results.append(result)
        if result["success"]:
            log_message(
                f"\n✅ 服务已恢复!熔断器应该会逐渐关闭。\n",
                "SUCCESS"
            )
            break
        time.sleep(1)

    log_message("\n" + "=" * 80, "INFO")
    log_message("✨ 测试完成!", "INFO")
    log_message("=" * 80, "INFO")

if __name__ == "__main__":
    main()
