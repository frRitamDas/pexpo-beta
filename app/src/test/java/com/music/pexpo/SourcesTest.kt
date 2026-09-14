package com.music.pexpo

import android.net.Uri
import com.music.pexpo.data.sources.MusicSource
import com.music.pexpo.data.sources.SourceResolver
import com.music.pexpo.data.sources.SourceStream
import com.music.pexpo.data.sources.StreamFormat
import com.music.pexpo.data.sources.StreamRequest
import com.music.pexpo.data.sources.TrackMatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// The rest of this file is intentionally preserved from the beta branch.
// The tie-ranking test below is made deterministic by asking bestAcross to
// collect the complete simultaneous batch before ranking it.

