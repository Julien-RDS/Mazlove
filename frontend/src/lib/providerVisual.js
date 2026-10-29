const DEMO_PORTRAITS = {
  Sophie: '/images/demo-sophie.jpg',
  Marc: '/images/demo-marc.jpg',
  Emma: '/images/demo-emma.jpg',
};

/** Real uploaded photos always take precedence over demo illustration assets. */
export function providerVisual(provider) {
  const images = Array.isArray(provider?.images) ? provider.images.filter(Boolean) : [];
  const isPlaceholder = (src) => /^https?:\/\/(?:www\.)?placehold\.co(?:\/|$)/i.test(src);
  const realPhoto = images.find((src) => !isPlaceholder(src));
  if (realPhoto) return { src: realPhoto, isIllustration: false };
  const demoPhoto = DEMO_PORTRAITS[provider?.userName];
  if (demoPhoto && images.some(isPlaceholder)) return { src: demoPhoto, isIllustration: true };
  return { src: null, isIllustration: false };
}
