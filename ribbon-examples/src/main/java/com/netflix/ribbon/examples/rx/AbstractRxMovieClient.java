/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.ribbon.examples.rx;

import com.netflix.hystrix.util.HystrixTimer;
import com.netflix.ribbon.examples.rx.common.Movie;
import io.netty.buffer.ByteBuf;
import rx.Notification;
import rx.Observable;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * Base class for the transport/template and proxy examples. It orchestrates application flow, and
 * handles result(s) from the server. The request execution is implemented in the derived classes, in a way
 * specific for each API abstration. This separation of concerns makes it is easy to compare complexity of different APIs.
 *
 * @author Tomasz Bak
 */
public abstract class AbstractRxMovieClient {
    protected static final String TEST_USER = "user1";
    protected static final Pattern NEW_LINE_SPLIT_RE = Pattern.compile("\n");

    protected abstract Observable<ByteBuf>[] triggerMoviesRegistration();

    protected abstract Observable<ByteBuf>[] triggerRecommendationsUpdate();

    protected abstract Observable<ByteBuf>[] triggerRecommendationsSearch();

    protected Observable<ByteBuf> registerMovies() {
        return Observable.concat(Observable.from(triggerMoviesRegistration()));
    }

    protected Observable<ByteBuf> updateRecommendations() {
        return Observable.concat(Observable.from(triggerRecommendationsUpdate()));
    }

    protected Observable<Void> searchCatalog() {
        List<String> searches = new ArrayList<>(2);
        Collections.addAll(searches, "findById", "findRawMovieById", "findMovie(name, category)");

        return Observable
            .concat(Observable.from(triggerRecommendationsSearch()))
            .flatMap(byteBuf -> {
                List<Movie> movies = new ArrayList<Movie>();
                String lines = byteBuf.toString(Charset.defaultCharset());
                for (String line: NEW_LINE_SPLIT_RE.split(lines)) {
                    movies.add(Movie.from(line));
                }
                return Observable.just(movies);
            })
            .zipWith(searches, (movies, query) -> {
            System.out.println(format("    %s=%s", query, movies));
            return null;
        });
    }

    public boolean runExample() {
        boolean allGood = true;
        try {
            System.out.println("Registering movies...");

            Notification<Void> result = executeServerCalls();
            allGood = !result.isOnError();
            if (allGood) {
                System.out.println("Application finished");
            } else {
                System.err.println("ERROR: execution failure");
                result.getThrowable().printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            allGood = false;
        } finally {
            shutdown();
        }
        return allGood;
    }

    Notification<Void> executeServerCalls() {
        Observable<Void> resultObservable = registerMovies().materialize().flatMap(
                notif -> {
                    if (!verifyStatus(notif)) {
                        return Observable.error(notif.getThrowable());
                    }
                    System.out.print("Updating user recommendations...");
                    return updateRecommendations().materialize().flatMap(
                            notif -> {
                                if (!verifyStatus(notif)) {
                                    return Observable.error(notif.getThrowable());
                                }
                                System.out.println("Searching through the movie catalog...");
                                return searchCatalog();
                            });
                }
        );
        return resultObservable.materialize().toBlocking().last();
    }

    protected void shutdown() {
        HystrixTimer.reset();
    }

    private static boolean verifyStatus(Notification<ByteBuf> notif) {
        if (notif.isOnError()) {
            System.out.println("ERROR");
            return false;
        }
        System.out.println("DONE");
        return true;
    }
}
