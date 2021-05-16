Sets /! Sets1;
Sets /0 Sets1;
Sets /1 Sets1;
Sets /= Sets1;
Sets /> Sets1;
Sets /< Sets1;
Sets /^ Sets1;

AB /^ [_s=s1] @* BC /= Sets;
AB /^ [_s=s1] v BC /= Sets;
(ABC * (AB v [m])') /= Sets;
(ABC /^ [_s=s1] @* AB') /= Sets;

-- Sets that have nonempty intersection with (some of) their elements:
IMSets = Sets /^ [_s=s1] /^ Sets /^ [s1=m] ^ Sets;
IMSets;

-- Sets that have nonempty intersection with all their elements:
(IMSets /^ [_s=s1] /= Sets ^ [_s=s1]) v [_s];
