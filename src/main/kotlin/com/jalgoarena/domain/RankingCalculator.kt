package com.jalgoarena.domain

import com.jalgoarena.data.SubmissionsRepository
import org.springframework.stereotype.Service
import javax.inject.Inject

@Service
class RankingCalculator(@Inject val repository: SubmissionsRepository) {

    fun ranking(users: Array<User>): List<RankEntry> {
        val submissions = repository.findAll()

        val bonusPoints = calculateBonusPointsForFastestSolutions(submissions, users)

        return users.map { user ->
            val userSubmissions = submissions.filter { it.userId == user.id }
            val solvedProblems = userSubmissions.map { it.problemId }

            RankEntry(
                    user.username,
                    score(userSubmissions) + bonusPoints[user.id] as Double,
                    solvedProblems,
                    user.region,
                    user.team
            )
        }
    }

    fun problemRanking(problemId: String, users: Array<User>): List<ProblemRankEntry> {
        val problemSubmissions = repository.findByProblemId(problemId)

        val bonusPoints = calculateBonusPointsForFastestSolutions(problemSubmissions, users)

        return problemSubmissions.map { submission ->
            val user = users.filter { it.id == submission.userId }.first()

            ProblemRankEntry(
                    user.username,
                    score(listOf(submission)) + bonusPoints[user.id] as Double,
                    submission.elapsedTime,
                    submission.language
            )
        }.sortedBy { it.elapsedTime }
    }

    private fun score(userSubmissions: List<Submission>): Double {
        fun timeFactor(elapsedTime: Double) =
                if (elapsedTime > 500) 1
                else if (elapsedTime > 100) 3
                else if (elapsedTime > 10) 5
                else if (elapsedTime >= 1) 8
                else 10

        return userSubmissions.sumByDouble {
            val languageFactor = if ("kotlin" == it.language) 1.5 else 1.0
            it.level * timeFactor(it.elapsedTime) * languageFactor
        }
    }

    private fun calculateBonusPointsForFastestSolutions(submissions: List<Submission>, users: Array<User>): Map<String, Double> {

        val bonusPoints = mutableMapOf<String, Double>()
        users.forEach { bonusPoints[it.id] = 0.0 }

        val problems = submissions.map { it.problemId }.distinct()

        problems.forEach { problem ->
            val problemSubmissions = submissions.filter { it.problemId == problem }
            val fastestSubmission = problemSubmissions.minBy { it.elapsedTime }

            if (fastestSubmission != null) {
                bonusPoints[fastestSubmission.userId] = bonusPoints[fastestSubmission.userId] as Double + 1.0
            }
        }

        return bonusPoints
    }
}