import React, { useState, useEffect } from 'react';
import { ChevronLeft, Search, Heart, Star, Filter, ArrowRight } from 'lucide-react';

const CuddleApp = () => {
  const [currentScreen, setCurrentScreen] = useState('home');
  const [selectedHeight, setSelectedHeight] = useState('1m72');
  const [customizations, setCustomizations] = useState({
    face: 'selected',
    chest: 'medium',
    hips: 'medium',
    thighs: 'medium'
  });
  const [searchQuery, setSearchQuery] = useState('');
  const [isSearching, setIsSearching] = useState(false);

  const profiles = [
    {
      name: 'Emily',
      age: 19,
      rating: 4.9,
      reviews: 324,
      price: 139,
      tags: ['Naturelle', '1m72', 'Sportive'],
      image: '/api/placeholder/200/300'
    },
    {
      name: 'Sophia',
      age: 22,
      rating: 4.1,
      reviews: 624,
      price: 159,
      tags: ['Naturelle', '1m72'],
      image: '/api/placeholder/200/300'
    }
  ];

  const customProfile = {
    name: 'Lila',
    match: '1m71',
    tags: ['Brune'],
    image: '/api/placeholder/300/400'
  };

  const handleSearch = () => {
    setIsSearching(true);
    setTimeout(() => {
      setCurrentScreen('results');
      setIsSearching(false);
    }, 2000);
  };

  const ScreenOne = () => (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      {/* Header */}
      <div className="flex items-center justify-between p-4 bg-white">
        <button className="flex items-center text-gray-600">
          <span className="text-sm">Traits physiques</span>
          <ChevronLeft className="w-4 h-4 ml-1 rotate-180" />
        </button>
      </div>

      {/* Body silhouette */}
      <div className="flex-1 flex flex-col items-center justify-center px-8">
        <div className="relative">
          {/* Silhouette placeholder */}
          <div className="w-48 h-80 bg-gray-200 rounded-full opacity-60 mb-8"></div>
        </div>

        <div className="text-center">
          <p className="text-gray-500 text-sm mb-2">Définissez la taille</p>
          <p className="text-3xl font-bold text-gray-800 mb-8">{selectedHeight}</p>
        </div>

        <button 
          onClick={() => setCurrentScreen('customize')}
          className="bg-pink-500 text-white px-8 py-3 rounded-full font-medium"
        >
          Continuer
        </button>
      </div>
    </div>
  );

  const ScreenTwo = () => (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      {/* Header */}
      <div className="flex items-center justify-between p-4 bg-white">
        <button onClick={() => setCurrentScreen('home')} className="p-2">
          <ChevronLeft className="w-5 h-5 text-gray-600" />
        </button>
        <button className="flex items-center text-gray-600">
          <span className="text-sm">Traits physiques</span>
          <ChevronLeft className="w-4 h-4 ml-1 rotate-180" />
        </button>
      </div>

      {/* Body with customization points */}
      <div className="flex-1 flex items-center justify-center px-8">
        <div className="relative">
          {/* Silhouette */}
          <div className="w-48 h-80 bg-gray-200 rounded-full opacity-60 relative">
            {/* Customization points */}
            <div className="absolute top-12 left-8">
              <div className="flex items-center">
                <div className="w-3 h-3 bg-black rounded-full mr-2"></div>
                <span className="bg-black text-white px-2 py-1 rounded text-xs">Face</span>
              </div>
            </div>
            
            <div className="absolute top-32 left-8">
              <div className="flex items-center">
                <div className="w-3 h-3 bg-black rounded-full mr-2"></div>
                <span className="bg-black text-white px-2 py-1 rounded text-xs">Poitrine</span>
              </div>
            </div>
            
            <div className="absolute top-48 left-8">
              <div className="flex items-center">
                <div className="w-3 h-3 bg-black rounded-full mr-2"></div>
                <span className="bg-black text-white px-2 py-1 rounded text-xs">Hanches</span>
              </div>
            </div>
            
            <div className="absolute top-64 left-8">
              <div className="flex items-center">
                <div className="w-3 h-3 bg-black rounded-full mr-2"></div>
                <span className="bg-black text-white px-2 py-1 rounded text-xs">Cuisse</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="p-4">
        <button 
          onClick={() => setCurrentScreen('search')}
          className="w-full bg-pink-500 text-white py-3 rounded-full font-medium"
        >
          Rechercher
        </button>
      </div>
    </div>
  );

  const ScreenThree = () => (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white p-4">
        <div className="flex items-center justify-between mb-4">
          <button onClick={() => setCurrentScreen('customize')} className="p-2">
            <ChevronLeft className="w-5 h-5 text-gray-600" />
          </button>
          <button className="flex items-center text-gray-600">
            <span className="text-sm">Traits physiques</span>
            <ChevronLeft className="w-4 h-4 ml-1 rotate-180" />
          </button>
        </div>

        {/* Search bar */}
        <div className="relative mb-4">
          <Search className="absolute left-3 top-3 w-4 h-4 text-gray-400" />
          <input
            type="text"
            placeholder="Rechercher votre désir"
            className="w-full pl-10 pr-4 py-2 bg-gray-100 rounded-full text-sm"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>

        {/* Filters */}
        <div className="flex items-center gap-4 mb-4">
          <button className="flex items-center gap-1 text-sm text-gray-600">
            <Heart className="w-4 h-4" />
            Calins
          </button>
          <button className="text-sm text-gray-600">Les mieux notés</button>
          <button className="flex items-center gap-1 text-sm text-gray-600">
            Durée
            <ChevronLeft className="w-3 h-3 rotate-180" />
          </button>
          <button className="text-sm text-gray-600">Tr...</button>
        </div>

        <p className="text-xs text-gray-400 mb-4">
          Découvrez comment les profils sont sélectionnés. En savoir plus
        </p>

        {/* Sponsored content */}
        <div className="bg-gray-100 p-3 rounded-lg mb-4 flex items-center justify-between">
          <div>
            <p className="font-medium text-sm">À découvrir sur Mazlove</p>
            <p className="text-xs text-gray-500">Sponsorisé</p>
          </div>
          <ArrowRight className="w-4 h-4 text-gray-400" />
        </div>
      </div>

      {/* Profiles grid */}
      <div className="p-4 grid grid-cols-2 gap-4">
        {profiles.map((profile, index) => (
          <div key={index} className="bg-black rounded-lg overflow-hidden relative">
            <div className="h-48 bg-gray-800 relative">
              <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent"></div>
              <div className="absolute bottom-3 left-3 text-white">
                <p className="font-medium">{profile.name} {profile.age}</p>
                <div className="flex items-center gap-1 text-xs">
                  <Star className="w-3 h-3 fill-yellow-400 text-yellow-400" />
                  <span>{profile.rating}</span>
                  <span className="text-gray-300">({profile.reviews})</span>
                </div>
              </div>
              <div className="absolute top-3 right-3 bg-black/50 text-white px-2 py-1 rounded text-xs">
                {profile.price}€
              </div>
            </div>
            <div className="p-2">
              <div className="flex flex-wrap gap-1">
                {profile.tags.map((tag, i) => (
                  <span key={i} className="text-xs bg-gray-100 px-2 py-1 rounded">
                    {tag}
                  </span>
                ))}
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="p-4">
        <button 
          onClick={handleSearch}
          className="w-full bg-pink-500 text-white py-3 rounded-full font-medium flex items-center justify-center gap-2"
        >
          {isSearching ? (
            <>
              <div className="animate-spin rounded-full h-4 w-4 border-2 border-white border-t-transparent"></div>
              Recherche en cours...
            </>
          ) : (
            'Trouver mon calin idéal'
          )}
        </button>
      </div>
    </div>
  );

  const ScreenFour = () => (
    <div className="min-h-screen bg-gradient-to-br from-pink-400 via-purple-500 to-pink-600">
      {/* Header */}
      <div className="flex items-center justify-between p-4 text-white">
        <div className="flex items-center gap-2">
          <div className="flex gap-1">
            <div className="w-1 h-1 bg-white rounded-full"></div>
            <div className="w-1 h-1 bg-white rounded-full"></div>
            <div className="w-1 h-1 bg-white rounded-full"></div>
          </div>
          <span className="text-sm">9:41</span>
        </div>
        <div className="flex items-center gap-1">
          <div className="flex gap-1">
            <div className="w-1 h-3 bg-white rounded-full"></div>
            <div className="w-1 h-3 bg-white rounded-full"></div>
            <div className="w-1 h-3 bg-white rounded-full"></div>
            <div className="w-1 h-3 bg-white rounded-full"></div>
          </div>
          <div className="ml-2 text-xs">📶 🔋</div>
        </div>
      </div>

      <div className="text-center text-white mb-8 px-4">
        <p className="text-lg font-light">1 résultat</p>
      </div>

      {/* Main profile */}
      <div className="flex-1 flex flex-col items-center justify-center px-8">
        <div className="relative mb-6">
          <div className="w-64 h-80 bg-white/10 backdrop-blur-sm rounded-3xl overflow-hidden">
            <div className="h-full bg-gradient-to-b from-transparent to-black/30 relative">
              {/* Profile image placeholder */}
              <div className="h-full w-full bg-gray-300 opacity-20"></div>
            </div>
          </div>

          {/* Match indicators */}
          <div className="absolute top-4 left-4 flex items-center gap-2 text-white">
            <div className="w-2 h-2 bg-white rounded-full"></div>
            <span className="text-xs bg-black/30 px-2 py-1 rounded-full">1m71</span>
          </div>
          
          <div className="absolute top-4 right-4 flex items-center gap-2 text-white">
            <div className="w-2 h-2 bg-white rounded-full"></div>
            <span className="text-xs bg-black/30 px-2 py-1 rounded-full">Brune</span>
          </div>

          <div className="absolute bottom-6 left-6 text-white">
            <h1 className="text-4xl font-light mb-2">{customProfile.name}</h1>
          </div>
        </div>

        <button className="bg-white text-purple-600 px-8 py-3 rounded-full font-medium shadow-lg">
          Voir le profil
        </button>
      </div>

      {/* Bottom navigation */}
      <div className="p-4">
        <button 
          onClick={() => setCurrentScreen('home')}
          className="w-full bg-white/20 backdrop-blur-sm text-white py-3 rounded-full font-medium"
        >
          Nouvelle recherche
        </button>
      </div>
    </div>
  );

  const renderScreen = () => {
    switch (currentScreen) {
      case 'home': return <ScreenOne />;
      case 'customize': return <ScreenTwo />;
      case 'search': return <ScreenThree />;
      case 'results': return <ScreenFour />;
      default: return <ScreenOne />;
    }
  };

  return (
    <div className="max-w-sm mx-auto bg-white shadow-xl">
      {renderScreen()}
    </div>
  );
};

export default CuddleApp;
