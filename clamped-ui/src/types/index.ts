export interface ClampedEvent {
  id: number
  timestamp: string
  appName: string
  environment: string
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
  tag: string | null
  message: string
  exceptionClass: string | null
  stacktrace: string | null
  metadata: string | null  // raw JSON string from DB
  sourceFile: string | null
  sourceLine: number | null
  sourceMethod: string | null
  threadName: string | null
  host: string | null
  status: 'OPEN' | 'IN_PROGRESS' | 'RESOLVED'
  fingerprint: string
  occurrenceCount: number
  firstSeen: string
  resolutionNotes: string | null
}

export interface TagCount {
  tag: string
  count: number
}

export interface TimelinePoint {
  hour: string
  count: number
}

export interface StatsResponse {
  countBySeverity: Record<string, number>
  countByStatus: Record<string, number>
  topTags: TagCount[]
  topExceptionClasses: TagCount[]
  topSourceLocations: TagCount[]
  timeline: TimelinePoint[]
}
