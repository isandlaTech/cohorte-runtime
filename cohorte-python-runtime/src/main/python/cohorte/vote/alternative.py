#!/usr/bin/env python
# -- Content-Encoding: UTF-8 --
"""
Voting engine: Alternative vote

See C.G.P. Grey video on Alternative vote

:author: Thomas Calmant
:copyright: Copyright 2013, isandlaTech
:license: GPLv3
:version: 1.0.0

..

    This file is part of Cohorte.

    Cohorte is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Cohorte is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Cohorte. If not, see <http://www.gnu.org/licenses/>.
"""

# Module version
__version_info__ = (1, 0, 0)
__version__ = ".".join(str(x) for x in __version_info__)

# Documentation strings format
__docformat__ = "restructuredtext en"

# ------------------------------------------------------------------------------

# Voting system
import cohorte.vote

# iPOPO Decorators
from pelix.ipopo.decorators import ComponentFactory, Provides, Instantiate, \
    Property

# Standard library
import collections
import logging
import math

# ------------------------------------------------------------------------------

_logger = logging.getLogger(__name__)

# ------------------------------------------------------------------------------


@ComponentFactory()
@Provides(cohorte.vote.SERVICE_VOTE_ENGINE)
@Property('_kind', cohorte.vote.PROP_VOTE_KIND, 'alternative')
@Instantiate('vote-engine-alternative')
class AlternativeEngine(object):
    """
    Voting system core service
    """
    def __init__(self):
        """
        Sets up members
        """
        # Supported kind of vote
        self._kind = None

    def get_kind(self):
        """
        Returns supported kind of vote
        """
        return self._kind

    def get_options(self):
        """
        Returns the options available for this engine

        :return: An option -> description dictionary
        """
        return {}

    def analyze(self, vote_round, ballots, candidates, parameters, vote_bean):
        """
        Analyzes the results of a vote

        :param vote_round: Round number (starts at 1)
        :param ballots: All ballots of the vote
        :param candidates: List of all candidates
        :param parameters: Parameters for the vote engine
        :param vote_bean: A VoteResults bean
        :return: The candidate(s) with the most votes
        """
        # Copy ballots results...
        results = {}
        candidates_ballots = {}

        for ballot in ballots:
            # Count supported 1st level candidates
            try:
                candidate = ballot.get_for()[0]

            except IndexError:
                # Elector did not vote
                pass

            else:
                ballot_for = collections.deque(ballot.get_for()[1:])
                candidates_ballots.setdefault(candidate, []).append(ballot_for)
                results[candidate] = results.get(candidate, 0) + 1

        while True:
            # Compute [(votes, candidate)] list
            plain_results = vote_bean.set_results(results)

            # Compute the number of votes for absolute majority
            nb_votes = sum(votes[0] for votes in plain_results)
            majority = math.floor(nb_votes / 2) + 1

            best_candidate = plain_results[0]
            if best_candidate[0] >= majority:
                # Candidate elected
                return best_candidate[1]
            elif best_candidate[0] == majority - 1:
                # It's a tie: keep the first one in the sorted list
                return best_candidate[1]
            else:
                # Get the least successful candidate
                min_candidate = plain_results[-1][1]

                # Forget its results
                del results[min_candidate]

                # Transfer its ballots
                try:
                    candidate_ballots = candidates_ballots.pop(min_candidate)
                    for ballot_for in candidate_ballots:
                        try:
                            # Get the next vote of the electors of
                            # the excluded one
                            candidate = ballot_for.popleft()
                        except IndexError:
                            # No more vote for this candidate
                            pass
                        else:
                            candidates_ballots.setdefault(candidate, [])\
                                .append(ballot_for)
                            results[candidate] = results.get(candidate, 0) + 1
                except KeyError:
                    # No ballot to transfer (no vote at all)
                    pass