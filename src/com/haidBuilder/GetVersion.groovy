package com.haidBuilder

class GetVersion {
    def getVersion(String BUILD_NUMBER, String GIT_COMMIT) {
        return new Date().format( 'yyMM' ) + "_${BUILD_NUMBER}" + "_${GIT_COMMIT}"
    }
}
