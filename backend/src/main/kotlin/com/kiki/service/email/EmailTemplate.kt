package com.kiki.service.email

import com.kiki.entity.GameNotice
import java.time.LocalDateTime

/**
 * 이메일 템플릿 인터페이스
 */
interface EmailTemplate {
    /**
     * 이메일 제목 생성
     */
    fun generateSubject(data: Any): String
    
    /**
     * 이메일 내용 생성
     */
    fun generateContent(data: Any): String
}

/**
 * 일일 다이제스트 이메일 템플릿
 */
class DailyDigestEmailTemplate : EmailTemplate {
    
    override fun generateSubject(data: Any): String {
        require(data is List<*> && data.all { it is GameNotice }) { "데이터는 GameNotice 리스트여야 합니다" }
        
        @Suppress("UNCHECKED_CAST")
        val notices = data as List<GameNotice>
        val gameNames = notices.groupBy { it.game.name }.keys
        val today = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MM/dd"))
        
        return when {
            gameNames.size == 1 -> "[$today] ${gameNames.first()} 업데이트 ${notices.size}건"
            gameNames.size == 2 -> "[$today] ${gameNames.joinToString(", ")} 업데이트 ${notices.size}건"
            else -> "[$today] 게임 업데이트 ${notices.size}건 (${gameNames.size}개 게임)"
        }
    }
    
    override fun generateContent(data: Any): String {
        require(data is List<*> && data.all { it is GameNotice }) { "데이터는 GameNotice 리스트여야 합니다" }
        
        @Suppress("UNCHECKED_CAST")
        val notices = data as List<GameNotice>
        val noticesByGame = notices.groupBy { it.game.name }
        val currentDate = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm"))
        
        return buildString {
            append(getEmailHeader())
            append(getStatsSection(notices.size))
            
            // 게임별 공지사항 섹션 생성
            noticesByGame.forEach { (gameName, gameNotices) ->
                append(getGameSection(gameName, gameNotices))
            }
            
            append(getEmailFooter(currentDate))
        }
    }
    
    private fun getEmailHeader(): String = """
        <!DOCTYPE html>
        <html lang="ko">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>KIKI 게임 업데이트 알림</title>
            <style>
                ${getEmailStyles()}
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>🎮 KIKI</h1>
                    <p>게임 업데이트 알림 서비스</p>
                </div>
                <div class="content">
    """.trimIndent()
    
    private fun getStatsSection(noticeCount: Int): String = """
        <div class="stats">
            <div class="stats-number">${noticeCount}개</div>
            <div>새로운 공지사항이 있습니다!</div>
        </div>
    """.trimIndent()
    
    private fun getGameSection(gameName: String, gameNotices: List<GameNotice>): String {
        val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm")
        
        return buildString {
            append("""
                <div class="game-section">
                    <div class="game-title">
                        <div class="game-icon"></div>
                        $gameName (${gameNotices.size}건)
                    </div>
            """.trimIndent())
            
            gameNotices.forEach { notice ->
                val publishedDate = notice.publishedDate.format(dateFormatter)
                val summary = notice.summary?.let { 
                    if (it.length > 150) "${it.take(150)}..." else it 
                } ?: "공지사항 내용을 확인해보세요."
                
                append("""
                    <div class="notice-item">
                        <div class="notice-title">
                            <a href="${notice.url}" target="_blank">${notice.title}</a>
                        </div>
                        <div class="notice-meta">📅 $publishedDate</div>
                        <div class="notice-summary">$summary</div>
                    </div>
                """.trimIndent())
            }
            
            append("</div>")
        }
    }
    
    private fun getEmailFooter(currentDate: String): String = """
                </div>
                <div class="footer">
                    <p>📧 $currentDate 에 발송된 이메일입니다.</p>
                    <p>더 이상 이메일을 받고 싶지 않으시다면 
                        <a href="{{unsubscribe_url}}" class="unsubscribe-link">구독 취소</a>를 클릭해주세요.
                    </p>
                            <p>© 2025 KIKI. All rights reserved.</p>
                </div>
            </div>
        </body>
        </html>
    """.trimIndent()
    
    private fun getEmailStyles(): String = """
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            line-height: 1.6;
            color: #333;
            max-width: 600px;
            margin: 0 auto;
            padding: 20px;
            background-color: #f8f9fa;
        }
        .container {
            background-color: white;
            border-radius: 12px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
            overflow: hidden;
        }
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px 20px;
            text-align: center;
        }
        .header h1 {
            margin: 0;
            font-size: 28px;
            font-weight: 700;
        }
        .header p {
            margin: 10px 0 0 0;
            opacity: 0.9;
            font-size: 16px;
        }
        .content {
            padding: 30px 20px;
        }
        .game-section {
            margin-bottom: 40px;
            border-left: 4px solid #667eea;
            padding-left: 20px;
        }
        .game-title {
            font-size: 22px;
            font-weight: 600;
            color: #2c3e50;
            margin-bottom: 20px;
            display: flex;
            align-items: center;
        }
        .game-icon {
            width: 24px;
            height: 24px;
            background: #667eea;
            border-radius: 50%;
            margin-right: 10px;
        }
        .notice-item {
            background-color: #f8f9fa;
            border-radius: 8px;
            padding: 20px;
            margin-bottom: 15px;
            border: 1px solid #e9ecef;
            transition: all 0.3s ease;
        }
        .notice-item:hover {
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        }
        .notice-title {
            font-size: 18px;
            font-weight: 600;
            color: #2c3e50;
            margin-bottom: 8px;
        }
        .notice-title a {
            color: #2c3e50;
            text-decoration: none;
        }
        .notice-title a:hover {
            color: #667eea;
        }
        .notice-meta {
            font-size: 14px;
            color: #6c757d;
            margin-bottom: 10px;
        }
        .notice-summary {
            font-size: 15px;
            color: #495057;
            line-height: 1.5;
        }
        .footer {
            background-color: #f8f9fa;
            padding: 20px;
            text-align: center;
            border-top: 1px solid #e9ecef;
        }
        .footer p {
            margin: 5px 0;
            font-size: 14px;
            color: #6c757d;
        }
        .unsubscribe-link {
            color: #dc3545;
            text-decoration: none;
            font-weight: 500;
        }
        .unsubscribe-link:hover {
            text-decoration: underline;
        }
        .stats {
            background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
            color: white;
            padding: 15px;
            border-radius: 8px;
            margin-bottom: 30px;
            text-align: center;
        }
        .stats-number {
            font-size: 24px;
            font-weight: 700;
        }
    """.trimIndent()
}

/**
 * 구독 확인 이메일 템플릿
 */
class SubscriptionConfirmationEmailTemplate : EmailTemplate {
    
    override fun generateSubject(data: Any): String = "KIKI 구독이 완료되었습니다!"
    
    override fun generateContent(data: Any): String = """
        <!DOCTYPE html>
        <html lang="ko">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>KIKI 구독 완료</title>
            <style>
                ${getSubscriptionStyles()}
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>🎉 구독 완료!</h1>
                </div>
                
                <div class="content">
                    <div class="success-icon">✅</div>
                    <div class="message">
                        KIKI 게임 업데이트 알림 서비스 구독이 완료되었습니다!
                    </div>
                    
                    <div class="features">
                        <h3>🎮 앞으로 받아보실 내용:</h3>
                        <div class="feature-item">📱 승리의 여신 니케 업데이트 정보</div>
                        <div class="feature-item">🏰 마비노기 모바일 업데이트 정보</div>
                        <div class="feature-item">🌌 원신 업데이트 정보</div>
                        <div class="feature-item">⚔️ 명조 업데이트 정보</div>
                        <div class="feature-item">⏰ 30분마다 자동 확인하여 새로운 소식만 전달</div>
                        <div class="feature-item">📧 깔끔한 이메일 형태로 정리된 정보 제공</div>
                    </div>
                    
                    <p style="font-size: 16px; color: #495057;">
                        새로운 공지사항이 있을 때마다 이메일로 알려드리겠습니다.<br>
                        게임의 최신 소식을 놓치지 마세요! 🚀
                    </p>
                </div>
                
                <div class="footer">
                    <p>© 2024 KIKI. All rights reserved.</p>
                </div>
            </div>
        </body>
        </html>
    """.trimIndent()
    
    private fun getSubscriptionStyles(): String = """
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            line-height: 1.6;
            color: #333;
            max-width: 600px;
            margin: 0 auto;
            padding: 20px;
            background-color: #f8f9fa;
        }
        .container {
            background-color: white;
            border-radius: 12px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
            overflow: hidden;
        }
        .header {
            background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
            color: white;
            padding: 40px 20px;
            text-align: center;
        }
        .header h1 {
            margin: 0;
            font-size: 32px;
            font-weight: 700;
        }
        .content {
            padding: 40px 20px;
            text-align: center;
        }
        .success-icon {
            font-size: 64px;
            margin-bottom: 20px;
        }
        .message {
            font-size: 18px;
            color: #2c3e50;
            margin-bottom: 30px;
        }
        .features {
            background-color: #f8f9fa;
            border-radius: 8px;
            padding: 20px;
            margin: 20px 0;
            text-align: left;
        }
        .feature-item {
            margin: 10px 0;
            font-size: 16px;
        }
        .footer {
            background-color: #f8f9fa;
            padding: 20px;
            text-align: center;
            border-top: 1px solid #e9ecef;
            font-size: 14px;
            color: #6c757d;
        }
    """.trimIndent()
}
