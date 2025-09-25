package com.kiki.scraper

import com.kiki.entity.GameNotice

/**
 * 게임별 스크래핑을 위한 공통 인터페이스
 * 각 게임사별로 이 인터페이스를 구현하여 공지사항을 스크래핑합니다.
 */
interface GameScraper {
    
    /**
     * 게임 공지사항을 스크래핑합니다.
     * @return 스크래핑된 공지사항 목록
     * @throws ScrapingException 스크래핑 중 오류 발생 시
     */
    fun scrapeNotices(): List<GameNotice>
    
    /**
     * 게임 이름을 반환합니다.
     * @return 게임 이름
     */
    fun getGameName(): String
    
    /**
     * 게임 공식 사이트의 기본 URL을 반환합니다.
     * @return 기본 URL
     */
    fun getBaseUrl(): String
    
    /**
     * 공지사항 페이지의 URL을 반환합니다.
     * @return 공지사항 페이지 URL
     */
    fun getNoticeUrl(): String
}