package com.myounis.appsyncrxsupport

import com.amazonaws.mobileconnectors.appsync.AppSyncPrefetch
import com.amazonaws.mobileconnectors.appsync.AppSyncQueryWatcher
import com.amazonaws.mobileconnectors.appsync.AppSyncSubscriptionCall
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.cache.normalized.GraphQLStoreOperation
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.fetcher.ResponseFetcher
import com.apollographql.apollo.internal.util.Cancelable
import io.reactivex.rxjava3.annotations.CheckReturnValue
import io.reactivex.rxjava3.core.*
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.exceptions.Exceptions
import org.jetbrains.annotations.NotNull

class Rx3AppSync {

    companion object {

        /**
         * Converts an [AppSyncQueryWatcher] to an asynchronous Observable.
         *
         * @param watcher the AppSyncQueryWatcher to convert.
         * @param <T>     the value type
         * @return the converted Observable
         * @throws NullPointerException if watcher == null
        </T> */
        @NotNull
        @CheckReturnValue
        fun <T> from(@NotNull watcher: AppSyncQueryWatcher<T>): Observable<Response<T>> {
            checkNotNull(watcher, { "watcher == null" })
            return Observable.create { emitter ->
                cancelOnObservableDisposed(emitter, watcher)
                watcher.enqueueAndWatch(object : GraphQLCall.Callback<T>() {
                    override fun onResponse(response: Response<T>) {
                        if (!emitter.isDisposed) {
                            emitter.onNext(response)
                        }
                    }

                    override fun onFailure(e: ApolloException) {
                        Exceptions.throwIfFatal(e)
                        if (!emitter.isDisposed) {
                            emitter.onError(e)
                        }
                    }
                })
            }
        }

        /**
         * Converts an [GraphQLCall] to an [Observable]. The number of emissions this Observable will have is based
         * on the [ResponseFetcher] used with the call.
         *
         * @param call the GraphQLCall to convert
         * @param <T>  the value type.
         * @return the converted Observable
         * @throws NullPointerException if originalCall == null
        </T> */
        @CheckReturnValue
        fun <T> from(call: GraphQLCall<T>): Observable<Response<T>> {
            checkNotNull(call, { "call == null" })
            return Observable.create { emitter ->
                val clone: GraphQLCall<T> = call.clone()
                cancelOnObservableDisposed(emitter, clone)
                clone.enqueue(object : GraphQLCall.Callback<T>() {
                    override fun onResponse(response: Response<T>) {
                        if (!emitter.isDisposed) {
                            emitter.onNext(response)
                        }
                    }

                    override fun onFailure(e: ApolloException) {
                        Exceptions.throwIfFatal(e)
                        if (!emitter.isDisposed) {
                            emitter.onError(e)
                        }
                    }

                    override fun onStatusEvent(event: GraphQLCall.StatusEvent) {
                        if (event === GraphQLCall.StatusEvent.COMPLETED && !emitter.isDisposed) {
                            emitter.onComplete()
                        }
                    }
                })
            }
        }

        /**
         * Converts an [AppSyncPrefetch] to a synchronous Completable
         *
         * @param prefetch the AppSyncPrefetch to convert
         * @return the converted Completable
         * @throws NullPointerException if prefetch == null
         */
        @CheckReturnValue
        fun from(prefetch: AppSyncPrefetch): Completable {
            checkNotNull(prefetch, { "prefetch == null" })
            return Completable.create { emitter ->
                val clone: AppSyncPrefetch = prefetch.clone()
                cancelOnCompletableDisposed(emitter, clone)
                clone.enqueue(object : AppSyncPrefetch.Callback() {
                    override fun onSuccess() {
                        if (!emitter.isDisposed) {
                            emitter.onComplete()
                        }
                    }

                    override fun onFailure(e: ApolloException) {
                        Exceptions.throwIfFatal(e)
                        if (!emitter.isDisposed) {
                            emitter.onError(e)
                        }
                    }
                })
            }
        }

        @CheckReturnValue
        fun <T> from(call: AppSyncSubscriptionCall<T>): Flowable<Response<T>> {
            return from(call, BackpressureStrategy.LATEST)
        }

        @CheckReturnValue
        fun <T> from(
            call: AppSyncSubscriptionCall<T>,
            backpressureStrategy: BackpressureStrategy
        ): Flowable<Response<T>> {
            checkNotNull(call, { "originalCall == null" })
            checkNotNull(backpressureStrategy, { "backpressureStrategy == null" })
            return Flowable.create({ emitter ->
                val clone: AppSyncSubscriptionCall<T> = call.clone()
                cancelOnFlowableDisposed(emitter, clone)
                clone.execute(
                    object : AppSyncSubscriptionCall.Callback<T> {
                        override fun onResponse(response: Response<T>) {
                            if (!emitter.isCancelled) {
                                emitter.onNext(response)
                            }
                        }

                        override fun onFailure(e: ApolloException) {
                            Exceptions.throwIfFatal(e)
                            if (!emitter.isCancelled) {
                                emitter.onError(e)
                            }
                        }

                        override fun onCompleted() {
                            if (!emitter.isCancelled) {
                                emitter.onComplete()
                            }
                        }

                    }
                )
            }, backpressureStrategy)
        }

        /**
         * Converts an [GraphQLStoreOperation] to a Single.
         *
         * @param operation the GraphQLStoreOperation to convert
         * @param <T>       the value type
         * @return the converted Single
        </T> */
        @CheckReturnValue
        fun <T> from(operation: GraphQLStoreOperation<T>): Single<T> {
            checkNotNull(operation, { "operation == null" })
            return Single.create { emitter ->
                operation.enqueue(object : GraphQLStoreOperation.Callback<T> {
                    override fun onSuccess(result: T) {
                        if (!emitter.isDisposed) {
                            emitter.onSuccess(result)
                        }
                    }

                    override fun onFailure(t: Throwable?) {
                        if (!emitter.isDisposed) {
                            emitter.onError(t)
                        }
                    }
                })
            }
        }

        private fun cancelOnCompletableDisposed(
            emitter: CompletableEmitter,
            cancelable: Cancelable
        ) {
            emitter.setDisposable(getRx3Disposable(cancelable))
        }

        private fun <T> cancelOnObservableDisposed(
            emitter: ObservableEmitter<T>,
            cancelable: Cancelable
        ) {
            emitter.setDisposable(getRx3Disposable(cancelable))
        }

        private fun <T> cancelOnFlowableDisposed(
            emitter: FlowableEmitter<T>,
            cancelable: Cancelable
        ) {
            emitter.setDisposable(getRx3Disposable(cancelable))
        }

        private fun getRx3Disposable(cancelable: Cancelable): Disposable {
            return object : Disposable {
                override fun dispose() {
                    cancelable.cancel()
                }

                override fun isDisposed(): Boolean {
                    return cancelable.isCanceled
                }
            }
        }

    }

}