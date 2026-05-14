import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AthleteProfileService } from './athlete-profile.service';
import { ProfileRequest, ProfileResponse } from './profile.model';

describe('AthleteProfileService', () => {
  let service: AthleteProfileService;
  let httpMock: HttpTestingController;

  const mockProfileResponse: ProfileResponse = {
    id: 1,
    dateOfBirth: '1990-05-15',
    weightKg: 72.5,
    restingHR: 48,
    maxHR: 190,
    lthr: 168,
    thresholdPaceSecondsPerKm: 270,
    vo2Max: 58.5,
    fiveKSeconds: 1080,
    tenKSeconds: 2280,
    halfMarathonSeconds: 5100,
    marathonSeconds: 10800,
    hrProfile: {
      zones: [
        { zoneNumber: 1, name: 'Recovery', lowerBound: 48, upperBound: 134 },
        { zoneNumber: 2, name: 'Aerobic Endurance', lowerBound: 134, upperBound: 151 },
        { zoneNumber: 3, name: 'Aerobic Power', lowerBound: 151, upperBound: 159 },
        { zoneNumber: 4, name: 'Threshold', lowerBound: 159, upperBound: 171 },
        { zoneNumber: 5, name: 'Anaerobic Endurance', lowerBound: 171, upperBound: 178 },
        { zoneNumber: 6, name: 'Anaerobic Power', lowerBound: 178, upperBound: 190 }
      ]
    },
    paceProfile: {
      zones: [
        { zoneNumber: 1, name: 'Recovery', lowerBound: 375, upperBound: 900 },
        { zoneNumber: 2, name: 'Aerobic Endurance', lowerBound: 310, upperBound: 375 },
        { zoneNumber: 3, name: 'Aerobic Power', lowerBound: 290, upperBound: 310 },
        { zoneNumber: 4, name: 'Threshold', lowerBound: 265, upperBound: 290 },
        { zoneNumber: 5, name: 'Anaerobic Endurance', lowerBound: 243, upperBound: 265 },
        { zoneNumber: 6, name: 'Anaerobic Power', lowerBound: 150, upperBound: 243 }
      ]
    }
  };

  const mockProfileRequest: ProfileRequest = {
    dateOfBirth: '1990-05-15',
    weightKg: 72.5,
    restingHR: 48,
    maxHR: 190,
    lthr: 168,
    thresholdPaceSecondsPerKm: 270,
    vo2Max: 58.5,
    fiveKSeconds: 1080,
    tenKSeconds: 2280,
    halfMarathonSeconds: 5100,
    marathonSeconds: 10800
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(AthleteProfileService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getProfile', () => {
    it('should send a GET request to /api/athlete-profile', () => {
      service.getProfile().subscribe(response => {
        expect(response).toEqual(mockProfileResponse);
      });

      const req = httpMock.expectOne('/api/athlete-profile');
      expect(req.request.method).toBe('GET');
      req.flush(mockProfileResponse);
    });
  });

  describe('createProfile', () => {
    it('should send a POST request to /api/athlete-profile with the profile data', () => {
      service.createProfile(mockProfileRequest).subscribe(response => {
        expect(response).toEqual(mockProfileResponse);
      });

      const req = httpMock.expectOne('/api/athlete-profile');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(mockProfileRequest);
      req.flush(mockProfileResponse);
    });
  });

  describe('updateProfile', () => {
    it('should send a PUT request to /api/athlete-profile with the profile data', () => {
      service.updateProfile(mockProfileRequest).subscribe(response => {
        expect(response).toEqual(mockProfileResponse);
      });

      const req = httpMock.expectOne('/api/athlete-profile');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(mockProfileRequest);
      req.flush(mockProfileResponse);
    });
  });
});
