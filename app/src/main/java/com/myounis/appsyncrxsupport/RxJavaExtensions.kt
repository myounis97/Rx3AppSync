package com.myounis.appsyncrxsupport

import com.amazonaws.mobileconnectors.appsync.*
import com.amazonaws.mobileconnectors.appsync.cache.normalized.AppSyncStore
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.*
import com.apollographql.apollo.cache.normalized.GraphQLStoreOperation
import io.reactivex.rxjava3.annotations.CheckReturnValue
import io.reactivex.rxjava3.core.*
import com.apollographql.apollo.fetcher.ResponseFetcher


@JvmSynthetic
@CheckReturnValue
fun AppSyncPrefetch.rx(): Completable =
    Rx3AppSync.from(this)

@JvmSynthetic
@CheckReturnValue
fun <T> GraphQLStoreOperation<T>.rx(): Single<T> =
    Rx3AppSync.from(this)

@JvmSynthetic
@CheckReturnValue
fun <T> AppSyncQueryWatcher<T>.rx(): Observable<Response<T>> =
    Rx3AppSync.from(this)

@JvmSynthetic
@CheckReturnValue
fun <T> GraphQLCall<T>.rx(): Observable<Response<T>> =
    Rx3AppSync.from(this)

@JvmSynthetic
@CheckReturnValue
fun <T> AppSyncSubscriptionCall<T>.rx(
    backpressureStrategy: BackpressureStrategy = BackpressureStrategy.LATEST
): Flowable<Response<T>> = Rx3AppSync.from(this, backpressureStrategy)

/**
 * Creates a new [AppSyncQueryCall] call and then converts it to an [Observable].
 *
 * The number of emissions this Observable will have is based on the
 * [ResponseFetcher] used with the call.
 */
@JvmSynthetic
@CheckReturnValue
inline fun <D : Operation.Data, T, V : Operation.Variables> AWSAppSyncClient.rxQuery(
    query: Query<D, T, V>,
    configure: AppSyncQueryCall<T>.() -> AppSyncQueryCall<T> = { this }
): Observable<Response<T>> = query(query).configure().rx()

/**
 * Creates a new [AppSyncMutationCall] call and then converts it to a [Single].
 */
@JvmSynthetic
@CheckReturnValue
inline fun <D : Operation.Data, T, V : Operation.Variables> AWSAppSyncClient.rxMutate(
    mutation: Mutation<D, T, V>,
    configure: AppSyncMutationCall<T>.() -> AppSyncMutationCall<T> = { this }
): Single<Response<T>> = mutate(mutation).configure().rx().singleOrError()

/**
 * Creates a new [AppSyncMutationCall] call and then converts it to a [Single].
 *
 * Provided optimistic updates will be stored in [AppSyncStore]
 * immediately before mutation execution. Any [AppSyncQueryWatcher] dependent on the changed cache records will
 * be re-fetched.
 */
@JvmSynthetic
@CheckReturnValue
inline fun <D : Operation.Data, T, V : Operation.Variables> AWSAppSyncClient.rxMutate(
    mutation: Mutation<D, T, V>,
    withOptimisticUpdates: D,
    configure: AppSyncMutationCall<T>.() -> AppSyncMutationCall<T> = { this }
): Single<Response<T>> = mutate(mutation, withOptimisticUpdates).configure().rx().singleOrError()

/**
 * Creates a new [AppSyncSubscriptionCall] call and then converts it to a [Flowable].
 *
 * Back-pressure strategy can be provided via [backpressureStrategy] parameter. The default value is [BackpressureStrategy.LATEST]
 */
@JvmSynthetic
@CheckReturnValue
inline fun <D : Operation.Data, T, V : Operation.Variables> AWSAppSyncClient.rxSubscribe(
    subscription: Subscription<D, T, V>,
    backpressureStrategy: BackpressureStrategy = BackpressureStrategy.LATEST
): Flowable<Response<T>> = subscribe(subscription).rx(backpressureStrategy)