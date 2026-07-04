export interface CampusUniversity {
  id: string;
  name: string;
}

export interface CampusDepartment {
  id: string;
  name: string;
  universityId: string;
}

export const campusUniversities: CampusUniversity[] = [
  {
    id: "8d9f5b4a-8f3c-4e6b-9a7d-1c2e3f4a5b60",
    name: "COMSATS University Islamabad",
  },
];

export const campusDepartments: CampusDepartment[] = [
  {
    id: "7c8e4a3b-6d2f-4b5a-8c9e-0f1a2b3c4d50",
    name: "Computer Science",
    universityId: "8d9f5b4a-8f3c-4e6b-9a7d-1c2e3f4a5b60",
  },
];

export const campusSemesterOptions = Array.from(
  { length: 8 },
  (_, index) => ({
    label: `Semester ${index + 1}`,
    value: String(index + 1),
  }),
);
