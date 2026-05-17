export class PaceFormatUtils {
  /**
   * Converts seconds per km to MM:SS/km display string.
   * Example: 300 → "5:00", 265 → "4:25"
   */
  static secondsToPace(seconds: number): string {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
  }

  /**
   * Converts MM:SS/km display string to seconds per km.
   * Example: "5:00" → 300, "4:25" → 265
   * Returns null if the input is not a valid MM:SS format.
   */
  static paceToSeconds(pace: string): number | null {
    const match = pace.match(/^(\d+):([0-5]\d)$/);
    if (!match) {
      return null;
    }
    const minutes = parseInt(match[1], 10);
    const seconds = parseInt(match[2], 10);
    return minutes * 60 + seconds;
  }

  /**
   * Validates that a MM:SS/km string represents a pace within the allowed range
   * (150–900 seconds, i.e., 2:30–15:00/km).
   */
  static isValidPace(pace: string): boolean {
    const seconds = PaceFormatUtils.paceToSeconds(pace);
    if (seconds === null) {
      return false;
    }
    return seconds >= 150 && seconds <= 900;
  }
}
